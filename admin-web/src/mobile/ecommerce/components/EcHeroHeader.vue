<template>
  <div class="mec-hero">
    <svg class="mec-hero__bg-svg" viewBox="0 0 100 100" preserveAspectRatio="none">
      <defs>
        <linearGradient id="heroGradient" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stop-color="#dbeafe" />
          <stop offset="70%" stop-color="#eff6ff" />
          <stop offset="100%" stop-color="#ffffff" />
        </linearGradient>
      </defs>
      <rect fill="url(#heroGradient)" width="100" height="100" />
      <path
        d="M 8 5 C 5 8 3 15 3 22 L 3 78 C 3 85 5 92 8 95"
        fill="none"
        stroke="#2563eb"
        stroke-width="0.8"
        stroke-linecap="round"
        stroke-dasharray="3 2"
      />
      <path
        d="M 92 5 C 95 8 97 15 97 22 L 97 78 C 97 85 95 92 92 95"
        fill="none"
        stroke="#2563eb"
        stroke-width="0.8"
        stroke-linecap="round"
        stroke-dasharray="3 2"
      />
      <path
        d="M 5 8 C 10 5 20 5 35 5 C 50 5 70 5 90 5 C 95 5 97 7 95 10"
        fill="none"
        stroke="#2563eb"
        stroke-width="0.8"
        stroke-linecap="round"
      />
      <path
        d="M 5 92 C 10 95 20 95 35 95 C 50 95 70 95 90 95 C 95 95 97 93 95 90"
        fill="none"
        stroke="#2563eb"
        stroke-width="0.8"
        stroke-linecap="round"
      />
      <circle cx="15" cy="30" r="3" fill="#fbbf24" opacity="0.6" />
      <circle cx="85" cy="25" r="2" fill="#3b82f6" opacity="0.5" />
      <circle cx="20" cy="70" r="2.5" fill="#60a5fa" opacity="0.4" />
      <circle cx="80" cy="75" r="3" fill="#fbbf24" opacity="0.5" />
      <path
        d="M 10 60 Q 15 55 20 60 T 30 60"
        fill="none"
        stroke="#2563eb"
        stroke-width="0.3"
        opacity="0.3"
      />
      <path
        d="M 70 15 Q 75 10 80 15 T 90 15"
        fill="none"
        stroke="#3b82f6"
        stroke-width="0.3"
        opacity="0.3"
      />
    </svg>

    <div class="mec-hero__content">
      <div class="mec-hero__top-bar">
        <SchemeADoodleFrame shape="pill" color="#e63946" sketch class="mec-hero__online">
          <span class="mec-hero__online-text">{{ ec.t('mobile.home.onlineBadge') }}</span>
        </SchemeADoodleFrame>
      </div>

      <div class="mec-hero__headline">
        <div class="mec-hero__headline-border">
          <h1 class="mec-hero__title">{{ ec.t('ecommerce.workbenchTitle') }}</h1>
          <p class="mec-hero__subtitle">{{ ec.t('functions.items.ecommerce.desc') }}</p>
        </div>
      </div>

      <div class="mec-hero__mascot-wrap">
        <SchemeADoodleFrame shape="pill" color="#f59e0b" :stroke-width="4" sketch class="mec-hero__mascot-ring">
          <div class="mec-hero__mascot-inner">
            <img class="mec-hero__mascot" :src="schemeAAssets.pixelDog" alt="" />
            <span class="mec-hero__mascot-date">{{ currentDate }}</span>
          </div>
        </SchemeADoodleFrame>
        <div class="mec-hero__mascot-glow"></div>
      </div>

      <SchemeADoodleFrame shape="rect" color="#2563eb" sketch :stroke-width="3" :shadow="false" class="mec-hero__stats">
        <div class="mec-hero__stats-inner">
          <div class="mec-hero__stat">
            <span class="mec-hero__stat-value">{{ ec.pendingShipCount.value }}</span>
            <span class="mec-hero__stat-label">{{ ec.t('mobile.ecommerce.pendingShip') }}</span>
          </div>
          <div class="mec-hero__stat-divider"></div>
          <div class="mec-hero__stat">
            <span class="mec-hero__stat-value mec-hero__stat-value--money">{{ settlementRevenue }}</span>
            <span class="mec-hero__stat-label">{{ ec.t('ecommerce.home.monthRevenue') }}</span>
          </div>
          <div class="mec-hero__stat-divider"></div>
          <div class="mec-hero__stat">
            <span class="mec-hero__stat-value">{{ ec.settlementStatusLabel.value }}</span>
            <span class="mec-hero__stat-label">{{ ec.t('ecommerce.home.settlementStatus') }}</span>
          </div>
        </div>
      </SchemeADoodleFrame>


    </div>

    <div class="mec-hero__deco mec-hero__deco--star-1">
      <img :src="schemeAAssets.starYellow" alt="" />
    </div>
    <div class="mec-hero__deco mec-hero__deco--star-2">
      <img :src="schemeAAssets.starBlue" alt="" />
    </div>
    <div class="mec-hero__deco mec-hero__deco--squiggle">
      <img :src="schemeAAssets.squiggleBlue" alt="" />
    </div>
    <div class="mec-hero__deco mec-hero__deco--paperclip">
      <img :src="schemeAAssets.paperclip" alt="" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import { MOBILE_ECOMMERCE_KEY } from '../mobileEcommerceContext'
import { schemeAAssets } from '@/mobile/home/themes/scheme-a/assets'
import SchemeADoodleFrame from '@/mobile/home/themes/scheme-a/SchemeADoodleFrame.vue'

const ec = inject(MOBILE_ECOMMERCE_KEY)!

const currentDate = computed(() => {
  const d = new Date()
  const weekDays = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getMonth() + 1}月${d.getDate()}日 周${weekDays[d.getDay()]}`
})

const settlementRevenue = computed(() => {
  const revenue = ec.overviewCards.value.find(c => c.key === 'revenue')?.value
  return revenue || '¥0'
})
</script>

<style scoped lang="scss">
.mec-hero {
  position: relative;
  width: 100%;
  padding: max(16px, env(safe-area-inset-top)) 16px 20px;
  overflow: hidden;
}

.mec-hero__bg-svg {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
}

.mec-hero__content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mec-hero__top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mec-hero__online {
  :deep(.sa-doodle-frame__body) {
    padding: 6px 12px;
  }
}

.mec-hero__online-text {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 800;
  color: #fff;
  background: #e63946;
  padding: 4px 8px;
  border-radius: 999px;

  &::before {
    content: '';
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #22c55e;
    animation: pulseDot 1.5s ease-in-out infinite;
  }
}

@keyframes pulseDot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.8); }
}

.mec-hero__date {
  font-size: 13px;
  color: #2563eb;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.mec-hero__headline {
  text-align: center;
}

.mec-hero__headline-border {
  display: inline-block;
  padding: 16px 24px;
  border: 3px solid #2563eb;
  border-radius: 16px;
  background: #fff;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: -6px;
    left: 50%;
    transform: translateX(-50%);
    width: 40%;
    height: 4px;
    background: #2563eb;
    border-radius: 2px;
    transform: translateX(-50%) rotate(-2deg);
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -6px;
    right: 10%;
    width: 30%;
    height: 4px;
    background: #3b82f6;
    border-radius: 2px;
    transform: rotate(3deg);
  }
}

.mec-hero__title {
  margin: 0;
  font-size: 30px;
  font-weight: 800;
  color: #2563eb;
  letter-spacing: 0.04em;
}

.mec-hero__subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #64748b;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.mec-hero__mascot-wrap {
  position: relative;
  display: flex;
  justify-content: center;
  margin-top: 4px;
}

.mec-hero__mascot-ring {
  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.mec-hero__mascot-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: #fff;
  animation: floatMascot 3s ease-in-out infinite;
}

@keyframes floatMascot {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.mec-hero__mascot {
  display: block;
  width: 50px;
  height: 50px;
  object-fit: contain;
}

.mec-hero__mascot-date {
  font-size: 10px;
  font-weight: 800;
  color: #f59e0b;
  margin-top: 2px;
  letter-spacing: 0.02em;
  text-align: center;
}

.mec-hero__mascot-glow {
  position: absolute;
  inset: -8px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(251, 191, 36, 0.3) 0%, transparent 70%);
  animation: glowPulse 2s ease-in-out infinite;
}

@keyframes glowPulse {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 0.8; transform: scale(1.1); }
}

.mec-hero__stats {
  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.mec-hero__stats-inner {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 14px 16px;
  background: #fff;
  border-radius: 16px;
}

.mec-hero__stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.mec-hero__stat-value {
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
  letter-spacing: 0.02em;

  &--money {
    color: #2563eb;
    font-size: 17px;
  }
}

.mec-hero__stat-label {
  font-size: 11px;
  color: #64748b;
  font-weight: 600;
  text-align: center;
}

.mec-hero__stat-divider {
  width: 1px;
  height: 36px;
  background: #e2e8f0;
}

.mec-hero__deco {
  position: absolute;
  z-index: 0;
  pointer-events: none;

  img {
    display: block;
    width: 100%;
    height: auto;
  }
}

.mec-hero__deco--star-1 {
  top: 24px;
  left: 12px;
  width: 36px;
  animation: twinkle 2.5s ease-in-out infinite;
}

.mec-hero__deco--star-2 {
  top: 60px;
  right: 16px;
  width: 28px;
  animation: twinkle 2.5s ease-in-out infinite 0.8s;
}

.mec-hero__deco--squiggle {
  bottom: 40px;
  left: -8px;
  width: 60px;
  opacity: 0.6;
}

.mec-hero__deco--paperclip {
  top: 10px;
  right: 20px;
  width: 28px;
  opacity: 0.8;
  transform: rotate(-15deg);
}

@keyframes twinkle {
  0%, 100% { opacity: 0.4; transform: scale(1) rotate(0deg); }
  50% { opacity: 0.9; transform: scale(1.1) rotate(5deg); }
}
</style>