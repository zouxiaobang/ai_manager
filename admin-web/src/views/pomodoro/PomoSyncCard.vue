<template>
  <div class="sync-card-wrap">
    <div class="sync-card-jagged">
      <div class="sync-card-jagged__inner">
        <header class="sync-card__title">
          <span class="sync-card__spark">✦</span>
          {{ t('pomodoro.timer.syncStatusTitle') }}
          <span class="sync-card__spark">✦</span>
        </header>

        <div class="sync-card__row">
          <!-- 左侧：当前控制 -->
          <div class="sync-card__status sync-card__status--control">
            <p class="sync-card__control-prefix">{{ t('pomodoro.timer.syncControllerPrefix') }}</p>
            <p class="sync-card__control-who">{{ controlWho }}</p>
          </div>

          <div class="sync-card__divider" aria-hidden="true" />

          <div class="sync-card__main">
            <!-- 管理端 -->
            <div class="sync-card__end">
              <div class="sync-card__icon-slot">
                <img
                  class="sync-card__icon"
                  src="/icons/pomodoro/sync-admin-pc.png"
                  :alt="t('pomodoro.timer.syncPanelAdmin')"
                  width="78"
                  height="56"
                />
              </div>
              <span class="sync-card__end-label">{{ t('pomodoro.timer.syncPanelAdmin') }}</span>
            </div>

            <!-- 同步区 -->
            <div class="sync-card__bridge">
            <div class="sync-card__link-row sync-card__link-row--dashed">
              <span class="sync-card__link sync-card__link--dash" aria-hidden="true" />
              <svg class="sync-card__sync-icon" viewBox="0 0 12 12" shape-rendering="crispEdges" aria-hidden="true">
                <g fill="#8bc34a">
                  <rect x="3" y="0" width="6" height="3" />
                  <rect x="6" y="3" width="3" height="3" />
                  <rect x="9" y="3" width="3" height="6" />
                  <rect x="6" y="6" width="6" height="3" />
                  <rect x="0" y="3" width="3" height="3" />
                  <rect x="0" y="6" width="3" height="3" />
                  <rect x="3" y="3" width="3" height="3" />
                  <rect x="3" y="9" width="6" height="3" />
                  <rect x="3" y="6" width="3" height="3" />
                  <rect x="0" y="6" width="3" height="6" />
                  <rect x="6" y="3" width="6" height="3" />
                  <rect x="9" y="6" width="3" height="3" />
                  <rect x="9" y="9" width="3" height="3" />
                  <rect x="6" y="9" width="3" height="3" />
                </g>
              </svg>
              <span class="sync-card__link sync-card__link--dash" aria-hidden="true" />
              <svg class="sync-card__pixel-arrow sync-card__pixel-arrow--right" viewBox="0 0 15 12" shape-rendering="crispEdges" aria-hidden="true">
                <g fill="#8bc34a">
                  <rect x="6" y="0" width="3" height="3" />
                  <rect x="3" y="3" width="9" height="3" />
                  <rect x="0" y="6" width="15" height="3" />
                  <rect x="3" y="9" width="9" height="3" />
                </g>
              </svg>
            </div>

            <div class="sync-card__link-row sync-card__link-row--solid">
              <svg class="sync-card__pixel-arrow sync-card__pixel-arrow--left" viewBox="0 0 15 12" shape-rendering="crispEdges" aria-hidden="true">
                <g fill="#8bc34a">
                  <rect x="6" y="0" width="3" height="3" />
                  <rect x="3" y="3" width="9" height="3" />
                  <rect x="0" y="6" width="15" height="3" />
                  <rect x="3" y="9" width="9" height="3" />
                </g>
              </svg>
              <span class="sync-card__link sync-card__link--solid" aria-hidden="true" />
              <span class="sync-card__health">{{ healthText }}</span>
              <span class="sync-card__link sync-card__link--solid" aria-hidden="true" />
            </div>

            <span class="sync-card__wifi-row" :class="{ 'is-on': online }">
              <svg class="sync-card__wifi-icon" viewBox="0 0 14 10" shape-rendering="crispEdges" aria-hidden="true">
                <g fill="currentColor">
                  <rect x="2" y="0" width="2" height="2" />
                  <rect x="10" y="0" width="2" height="2" />
                  <rect x="1" y="2" width="12" height="2" />
                  <rect x="2" y="4" width="10" height="2" />
                  <rect x="3" y="6" width="8" height="2" />
                  <rect x="6" y="8" width="2" height="2" />
                </g>
              </svg>
              WiFi
            </span>
          </div>

            <!-- 副屏 -->
            <div class="sync-card__end">
              <div class="sync-card__icon-slot sync-card__device">
                <img
                  class="sync-card__icon"
                  src="/icons/pomodoro/sync-device.png"
                  :alt="t('pomodoro.timer.syncPanelDevice')"
                  width="78"
                  height="56"
                />
                <span class="sync-card__device-time">{{ deviceTime }}</span>
              </div>
              <span class="sync-card__end-label sync-card__end-label--green">
                {{ t('pomodoro.timer.syncPanelDevice') }}
              </span>
            </div>
          </div>

          <div class="sync-card__divider" aria-hidden="true" />

          <!-- 右侧：副屏在线状态 -->
          <div class="sync-card__status sync-card__status--device">
            <p class="sync-card__device-who">{{ t('pomodoro.timer.syncPanelDevice') }}</p>
            <span class="sync-card__badge" :class="{ 'is-on': online }">
              <i class="sync-card__badge-dot" />
              {{ online ? t('pomodoro.timer.online') : t('pomodoro.timer.offline') }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  deviceTime: string
  healthText: string
  online: boolean
  controller: 'ADMIN' | 'DEVICE' | null
}>()

const { t } = useI18n()

const controlWho = computed(() => {
  if (props.controller === 'ADMIN') {
    return t('pomodoro.timer.syncControllerAdmin')
  }
  if (props.controller === 'DEVICE') {
    return t('pomodoro.timer.syncControllerDevice')
  }
  return '—'
})
</script>

<style scoped lang="scss">
$green: #8bc34a;
$tomato: #ef5350;
$frame: rgb(74 106 154 / 58%);
$bg: rgb(12 16 40 / 26%);
$step: 4px;
$jag-steps: 4;

// 四角各 4 级台阶锯齿（每级一个直角）
@mixin sync-card-jag-clip($n: 4) {
  clip-path: polygon(
    0 calc(var(--s) * #{$n}),
    var(--s) calc(var(--s) * #{$n}),
    var(--s) calc(var(--s) * #{$n - 1}),
    calc(var(--s) * 2) calc(var(--s) * #{$n - 1}),
    calc(var(--s) * 2) calc(var(--s) * #{$n - 2}),
    calc(var(--s) * 3) calc(var(--s) * #{$n - 2}),
    calc(var(--s) * 3) var(--s),
    calc(var(--s) * #{$n}) var(--s),
    calc(var(--s) * #{$n}) 0,
    calc(100% - var(--s) * #{$n}) 0,
    calc(100% - var(--s) * #{$n}) var(--s),
    calc(100% - var(--s) * 3) var(--s),
    calc(100% - var(--s) * 3) calc(var(--s) * #{$n - 2}),
    calc(100% - var(--s) * 2) calc(var(--s) * #{$n - 2}),
    calc(100% - var(--s) * 2) calc(var(--s) * #{$n - 1}),
    calc(100% - var(--s)) calc(var(--s) * #{$n - 1}),
    calc(100% - var(--s)) calc(var(--s) * #{$n}),
    100% calc(var(--s) * #{$n}),
    100% calc(100% - var(--s) * #{$n}),
    calc(100% - var(--s)) calc(100% - var(--s) * #{$n}),
    calc(100% - var(--s)) calc(100% - var(--s) * #{$n - 1}),
    calc(100% - var(--s) * 2) calc(100% - var(--s) * #{$n - 1}),
    calc(100% - var(--s) * 2) calc(100% - var(--s) * #{$n - 2}),
    calc(100% - var(--s) * 3) calc(100% - var(--s) * #{$n - 2}),
    calc(100% - var(--s) * 3) calc(100% - var(--s)),
    calc(100% - var(--s) * #{$n}) calc(100% - var(--s)),
    calc(100% - var(--s) * #{$n}) 100%,
    calc(var(--s) * #{$n}) 100%,
    calc(var(--s) * #{$n}) calc(100% - var(--s)),
    calc(var(--s) * 3) calc(100% - var(--s)),
    calc(var(--s) * 3) calc(100% - var(--s) * #{$n - 2}),
    calc(var(--s) * 2) calc(100% - var(--s) * #{$n - 2}),
    calc(var(--s) * 2) calc(100% - var(--s) * #{$n - 1}),
    var(--s) calc(100% - var(--s) * #{$n - 1}),
    var(--s) calc(100% - var(--s) * #{$n}),
    0 calc(100% - var(--s) * #{$n})
  );
}

.sync-card-wrap {
  display: flex;
  justify-content: center;
}

.sync-card-jagged {
  --s: #{$step};
  position: relative;
  width: min(100%, 620px);
  padding: var(--s);
  background: $frame;
  @include sync-card-jag-clip($jag-steps);
  filter: drop-shadow(0 0 8px rgb(74 106 154 / 35%));
}

.sync-card-jagged__inner {
  background: $bg;
  padding: 10px 12px 12px;
  @include sync-card-jag-clip($jag-steps);
}

.sync-card__title {
  margin: 0 0 8px;
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  color: $green;
  letter-spacing: 2px;
}

.sync-card__spark {
  color: $green;
  opacity: 0.9;
}

.sync-card__row {
  display: flex;
  align-items: center;
  gap: 0;
  width: 100%;
}

.sync-card__main {
  display: flex;
  flex: 1 1 auto;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 0;
  padding: 0 4px;
}

.sync-card__end {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  padding: 4px;
}

.sync-card__icon-slot {
  position: relative;
  flex-shrink: 0;
  width: 78px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sync-card__icon {
  display: block;
  width: 78px;
  height: 56px;
  object-fit: contain;
  image-rendering: pixelated;
  shape-rendering: crispEdges;
}

.sync-card__device-time {
  position: absolute;
  top: 36%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-family: 'Courier New', Consolas, monospace;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
  color: #f0f4ff;
  letter-spacing: -0.5px;
  pointer-events: none;
  text-shadow: 0 0 2px #000;
}

.sync-card__end-label {
  font-size: 13px;
  font-weight: 600;
  color: #c0d0e8;

  &--green {
    color: $green;
  }
}

.sync-card__bridge {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: center;
  flex: 1 1 auto;
  min-width: 96px;
  gap: 4px;
  padding: 0;
}

.sync-card__link-row {
  display: flex;
  align-items: center;
  gap: 5px;
  width: 100%;
}

.sync-card__link {
  position: relative;
  flex: 1;
  min-width: 4px;
}

.sync-card__link--dash {
  height: 3px;
  background: repeating-linear-gradient(
    to right,
    $green 0,
    $green 6px,
    transparent 6px,
    transparent 10px
  );
  opacity: 0.9;
}

.sync-card__link--solid {
  height: 3px;
  background: $green;
  opacity: 0.95;
}

.sync-card__sync-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  image-rendering: pixelated;
  shape-rendering: crispEdges;
}

.sync-card__pixel-arrow {
  flex-shrink: 0;
  width: 18px;
  height: 14px;
  image-rendering: pixelated;
  shape-rendering: crispEdges;

  &--left {
    transform: scaleX(-1);
  }
}

.sync-card__wifi-icon {
  width: 20px;
  height: 14px;
  image-rendering: pixelated;
  shape-rendering: crispEdges;
}

.sync-card__health {
  flex-shrink: 0;
  font-size: 14px;
  font-weight: 800;
  color: $green;
  white-space: nowrap;
  letter-spacing: 0.5px;
}

.sync-card__wifi-row {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  align-self: center;
  font-size: 13px;
  font-weight: 700;
  color: #607888;

  &.is-on {
    color: #90caf9;
  }
}

.sync-card__divider {
  width: 0;
  align-self: stretch;
  margin: 2px 4px;
  border-left: 2px dotted rgb(90 120 160 / 50%);
  flex-shrink: 0;
}

.sync-card__status {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  flex: 0 0 auto;
  width: auto;
  padding: 0 4px;

  &--control {
    padding-right: 2px;
  }

  &--device {
    padding-left: 2px;
  }
}

.sync-card__device-who {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.1;
  color: $green;
  text-shadow: 0 0 12px rgb(139 195 74 / 35%);
}

.sync-card__control-prefix {
  margin: 0;
  font-size: 14px;
  color: #90a4ae;
}

.sync-card__control-who {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.1;
  color: $tomato;
  text-shadow: 2px 2px 0 rgb(80 20 20 / 45%);
}

.sync-card__badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px 3px 7px;
  font-size: 14px;
  font-weight: 700;
  color: #607080;
  background: rgb(20 28 48 / 42%);
  border: 2px solid #2a3a50;
  border-radius: 0;
  clip-path: polygon(
    0 5px,
    5px 5px,
    5px 0,
    calc(100% - 5px) 0,
    calc(100% - 5px) 5px,
    100% 5px,
    100% calc(100% - 5px),
    calc(100% - 5px) calc(100% - 5px),
    calc(100% - 5px) 100%,
    5px 100%,
    5px calc(100% - 5px),
    0 calc(100% - 5px)
  );

  &.is-on {
    color: $green;
    border-color: rgb(139 195 74 / 45%);
  }
}

.sync-card__badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 0;
  background: #506070;

  .sync-card__badge.is-on & {
    background: $green;
    box-shadow: 0 0 6px $green;
  }
}

@media (max-width: 560px) {
  .sync-card__row {
    flex-wrap: wrap;
    justify-content: center;
  }

  .sync-card__main {
    flex: 1 1 100%;
    order: 2;
    padding: 6px 0;
  }

  .sync-card__divider {
    display: none;
  }

  .sync-card__status {
    align-items: center;
    width: calc(50% - 8px);

    &--control {
      order: 1;
      border-right: 1px dotted rgb(90 120 160 / 40%);
    }

    &--device {
      order: 3;
    }
  }
}
</style>
