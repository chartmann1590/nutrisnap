package com.charles.nutrisnap.ui.components

import com.charles.nutrisnap.data.badge.BadgeType

enum class PipAccessory(val displayName: String, val emoji: String, val unlockedBy: BadgeType?) {
    NONE("No accessory", "", null),
    CHEF_HAT("Chef Hat", "👨‍🍳", BadgeType.CHEF_HAT),
    PARTY_CROWN("Party Crown", "👑", BadgeType.HOT_STREAK),
    GOLDEN_LEAF("Golden Leaf", "🍃", BadgeType.ON_A_ROLL),
    HEART_GLASSES("Heart Glasses", "🕶️", BadgeType.BALANCED_DAY),
    CONFETTI_HALO("Confetti Halo", "🎊", BadgeType.CENTURY),
    RAINBOW_HALO("Rainbow Halo", "🌈", BadgeType.UNSTOPPABLE),
}
