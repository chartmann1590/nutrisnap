export interface Env {
	GH_API_TOKEN: string;
	APP_SHARED_SECRET: string;
	ADMIN_SECRET: string;
	GH_REPO_OWNER: string;
	GH_REPO_NAME: string;
	FEEDBACK_ASSETS_DIR: string;
	AI_REPORTS: KVNamespace;
}

interface AiReport {
	category: string;
	description: string;
	foodName: string;
	estimateJson: string;
	appVersion: string;
	receivedAt: number;
}

const GITHUB_API = "https://api.github.com";

function githubHeaders(env: Env): HeadersInit {
	return {
		Authorization: `Bearer ${env.GH_API_TOKEN}`,
		Accept: "application/vnd.github+json",
		"X-GitHub-Api-Version": "2022-11-28",
		"User-Agent": "NutriSnap-GithubProxy/1.0",
		"Content-Type": "application/json",
	};
}

async function timingSafeEqual(a: string, b: string): Promise<boolean> {
	const enc = new TextEncoder();
	const aBytes = enc.encode(a);
	const bBytes = enc.encode(b);
	if (aBytes.length !== bBytes.length) {
		// Still hash a dummy comparison so failure isn't distinguishable by length alone.
		await crypto.subtle.digest("SHA-256", aBytes);
		return false;
	}
	const key = await crypto.subtle.importKey(
		"raw",
		aBytes,
		{ name: "HMAC", hash: "SHA-256" },
		false,
		["sign"],
	);
	const macA = await crypto.subtle.sign("HMAC", key, aBytes);
	const macB = await crypto.subtle.sign("HMAC", key, bBytes);
	const viewA = new Uint8Array(macA);
	const viewB = new Uint8Array(macB);
	let diff = 0;
	for (let i = 0; i < viewA.length; i++) {
		diff |= viewA[i] ^ viewB[i];
	}
	return diff === 0;
}

function json(data: unknown, status = 200): Response {
	return new Response(JSON.stringify(data), {
		status,
		headers: { "Content-Type": "application/json" },
	});
}

function sanitizeFilename(name: string): string | null {
	// Single path segment only — no traversal, no nested directories.
	if (!/^[A-Za-z0-9._-]+$/.test(name)) return null;
	if (name === "." || name === "..") return null;
	return name;
}

async function forwardToGithub(env: Env, path: string, init: RequestInit): Promise<Response> {
	const upstream = await fetch(`${GITHUB_API}${path}`, {
		...init,
		headers: { ...githubHeaders(env), ...(init.headers ?? {}) },
	});
	const body = await upstream.text();
	return new Response(body, {
		status: upstream.status,
		headers: { "Content-Type": "application/json" },
	});
}

async function requireAdmin(request: Request, env: Env): Promise<boolean> {
	const header = request.headers.get("X-Admin-Secret") ?? "";
	return Boolean(env.ADMIN_SECRET) && (await timingSafeEqual(header, env.ADMIN_SECRET));
}

/** Report keys are `report:<epochMs>:<uuid>` so lexicographic KV listing is chronological. */
function reportKey(): string {
	return `report:${Date.now()}:${crypto.randomUUID()}`;
}

async function listReportsSince(env: Env, sinceMs: number): Promise<AiReport[]> {
	const reports: AiReport[] = [];
	let cursor: string | undefined;
	do {
		const page = await env.AI_REPORTS.list({ prefix: "report:", cursor, limit: 1000 });
		for (const key of page.keys) {
			const parts = key.name.split(":");
			const ts = Number(parts[1]);
			if (!Number.isFinite(ts) || ts < sinceMs) continue;
			const value = await env.AI_REPORTS.get(key.name);
			if (value) {
				try {
					reports.push(JSON.parse(value) as AiReport);
				} catch {
					// Skip malformed entries rather than fail the whole summary.
				}
			}
		}
		cursor = page.list_complete ? undefined : page.cursor;
	} while (cursor);
	return reports;
}

export default {
	async fetch(request: Request, env: Env): Promise<Response> {
		if (request.method === "OPTIONS") {
			return new Response(null, { status: 204 });
		}

		const url = new URL(request.url);

		// Admin-only trend summary — gated by a separate secret never shipped in the app,
		// so a leaked/reverse-engineered APK secret can't be used to read report data.
		if (request.method === "GET" && url.pathname === "/reports/summary") {
			if (!(await requireAdmin(request, env))) return json({ error: "unauthorized" }, 401);
			const sinceMs = Number(url.searchParams.get("sinceMs")) || Date.now() - 24 * 60 * 60 * 1000;
			const reports = await listReportsSince(env, sinceMs);
			const byCategory: Record<string, number> = {};
			for (const r of reports) {
				byCategory[r.category] = (byCategory[r.category] ?? 0) + 1;
			}
			return json({
				sinceMs,
				total: reports.length,
				byCategory,
				samples: reports.slice(-20),
			});
		}

		const authHeader = request.headers.get("X-App-Secret") ?? "";
		if (!env.APP_SHARED_SECRET || !(await timingSafeEqual(authHeader, env.APP_SHARED_SECRET))) {
			return json({ error: "unauthorized" }, 401);
		}

		// POST /reports — user flags an AI food-estimate response as wrong/inappropriate.
		if (request.method === "POST" && url.pathname === "/reports") {
			const body = await request.json<Record<string, unknown>>();
			const report: AiReport = {
				category: String(body.category ?? "other").slice(0, 64),
				description: String(body.description ?? "").slice(0, 2000),
				foodName: String(body.foodName ?? "").slice(0, 256),
				estimateJson: String(body.estimateJson ?? "").slice(0, 4000),
				appVersion: String(body.appVersion ?? "").slice(0, 32),
				receivedAt: Date.now(),
			};
			await env.AI_REPORTS.put(reportKey(), JSON.stringify(report), {
				expirationTtl: 60 * 60 * 24 * 90, // 90 days is plenty for trend analysis
			});
			return json({ ok: true });
		}

		const owner = env.GH_REPO_OWNER;
		const repo = env.GH_REPO_NAME;

		try {
			// POST /issues { title, body }
			if (request.method === "POST" && url.pathname === "/issues") {
				const payload = await request.json();
				return await forwardToGithub(env, `/repos/${owner}/${repo}/issues`, {
					method: "POST",
					body: JSON.stringify(payload),
				});
			}

			// GET /issues/:number
			const issueMatch = url.pathname.match(/^\/issues\/(\d+)$/);
			if (request.method === "GET" && issueMatch) {
				return await forwardToGithub(env, `/repos/${owner}/${repo}/issues/${issueMatch[1]}`, {
					method: "GET",
				});
			}

			// GET /issues/:number/comments
			const commentsMatch = url.pathname.match(/^\/issues\/(\d+)\/comments$/);
			if (request.method === "GET" && commentsMatch) {
				return await forwardToGithub(
					env,
					`/repos/${owner}/${repo}/issues/${commentsMatch[1]}/comments`,
					{ method: "GET" },
				);
			}

			// POST /issues/:number/comments { body }
			if (request.method === "POST" && commentsMatch) {
				const payload = await request.json();
				return await forwardToGithub(
					env,
					`/repos/${owner}/${repo}/issues/${commentsMatch[1]}/comments`,
					{ method: "POST", body: JSON.stringify(payload) },
				);
			}

			// PUT /assets/:filename { message, content } — restricted to the feedback-assets dir.
			const assetMatch = url.pathname.match(/^\/assets\/([^/]+)$/);
			if (request.method === "PUT" && assetMatch) {
				const filename = sanitizeFilename(decodeURIComponent(assetMatch[1]));
				if (!filename) return json({ error: "invalid filename" }, 400);
				const payload = await request.json();
				const path = `${env.FEEDBACK_ASSETS_DIR}/${filename}`;
				return await forwardToGithub(
					env,
					`/repos/${owner}/${repo}/contents/${path}`,
					{ method: "PUT", body: JSON.stringify(payload) },
				);
			}

			return json({ error: "not found" }, 404);
		} catch (err) {
			return json({ error: "proxy error", detail: String(err) }, 502);
		}
	},
} satisfies ExportedHandler<Env>;
