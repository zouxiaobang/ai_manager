<template>
  <div class="settlement-center">
    <div class="settlement-center__layout">
      <aside class="settlement-center__left">
        <header class="settlement-center__head">
          <h2 class="settlement-section-title">{{ t('ecommerce.monthlySettlement.workbenchTitle') }}</h2>
          <p class="settlement-center__subtitle">{{ t('ecommerce.monthlySettlement.workbenchSubtitle') }}</p>
        </header>

        <div class="settlement-center__controls">
          <MonthStepper
            v-model="settlementMonth"
            :placeholder="t('ecommerce.monthlySettlement.monthPlaceholder')"
          />
        </div>

        <h3 class="settlement-section-title">{{ t('ecommerce.monthlySettlement.prepChecklistTitle') }}</h3>

        <ul v-loading="prepLoading" class="prep-checklist">
          <li
            v-for="task in prepTasks"
            :key="task.key"
            class="prep-checklist__item"
            :class="[`is-${task.tone}`, { 'is-clickable': !!task.action }]"
            @click="onPrepTaskClick(task)"
          >
            <span class="prep-checklist__dot" :class="`is-${task.tone}`">
              <el-icon v-if="task.tone === 'success'"><Check /></el-icon>
              <el-icon v-else-if="task.tone === 'warning'"><Warning /></el-icon>
            </span>
            <div class="prep-checklist__body">
              <div class="prep-checklist__title-row">
                <div class="prep-checklist__title">{{ task.title }}</div>
                <el-tag
                  v-if="task.statusTag"
                  :type="task.statusTag.type"
                  effect="plain"
                  round
                  class="prep-checklist__status"
                >
                  {{ task.statusTag.label }}
                </el-tag>
              </div>
              <div v-if="task.desc || task.descHighlight" class="prep-checklist__desc">
                <template v-if="task.descHighlight">
                  {{ task.desc }}<span class="prep-checklist__desc-highlight">{{ task.descHighlight }}</span>
                </template>
                <template v-else>{{ task.desc }}</template>
              </div>
              <div v-if="task.expressBillCards?.length" class="prep-express-cards">
                <div
                  v-for="card in task.expressBillCards"
                  :key="card.id"
                  class="prep-express-card"
                >
                  <div
                    class="prep-express-card__avatar"
                    :class="{ 'is-custom': card.isCustomAvatar }"
                  >
                    <img :src="card.iconSrc" :alt="card.name" />
                  </div>
                  <div class="prep-express-card__body">
                    <div class="prep-express-card__name">{{ card.name }}</div>
                    <div class="prep-express-card__match">
                      <span class="prep-express-card__match-count">{{ card.matched }}/{{ card.total }}</span>{{ t('ecommerce.monthlySettlement.expressBillMatchSuffix') }}
                    </div>
                    <div v-if="card.gapCount" class="prep-express-card__gap">
                      {{ t('ecommerce.monthlySettlement.expressBillGap', { name: card.name, count: card.gapCount }) }}
                    </div>
                  </div>
                </div>
              </div>
              <div v-else-if="task.subItems?.length" class="prep-checklist__subs">
                <div v-for="(sub, idx) in task.subItems" :key="idx" class="prep-checklist__sub">
                  {{ sub }}
                </div>
              </div>
              <div v-if="task.lastOperationTime" class="prep-checklist__time">
                {{ t(task.lastTimeLabelKey ?? 'ecommerce.monthlySettlement.prepLastOperation', { time: task.lastOperationTime }) }}
              </div>
            </div>
            <el-icon v-if="task.action" class="prep-checklist__go" aria-hidden="true">
              <ArrowRight />
            </el-icon>
          </li>
        </ul>

        <el-tooltip
          :content="calculateButtonTooltip"
          :disabled="!calculateButtonTooltip"
          placement="top"
        >
          <span class="settlement-center__calculate-wrap">
            <el-button
              type="success"
              size="large"
              class="settlement-center__calculate"
              :class="`is-${calculateButtonMode}`"
              :loading="submitting"
              :disabled="calculateButtonDisabled"
              @click="onCalculate"
            >
              {{ calculateButtonLabel }}
            </el-button>
          </span>
        </el-tooltip>
        <p class="settlement-center__calculate-hint">
          {{ t('ecommerce.monthlySettlement.calculateAccuracyHint') }}
        </p>
      </aside>

      <main v-loading="calculating" class="settlement-center__right">
        <header class="settlement-center__result-head">
          <div class="settlement-center__result-intro">
            <div class="settlement-center__result-title-line">
              <h2 class="settlement-section-title">{{ t('ecommerce.monthlySettlement.resultTitle') }}</h2>
              <span v-if="settlementPeriodLabel" class="settlement-center__result-meta">
                {{ t('ecommerce.monthlySettlement.statisticsPeriod', { period: settlementPeriodLabel }) }}
              </span>
            </div>
          </div>
          <div v-if="lastCalculatedDisplay" class="settlement-center__result-actions">
            <span class="settlement-center__result-meta">
              {{ t('ecommerce.monthlySettlement.lastUpdatedAt', { time: lastCalculatedDisplay }) }}
            </span>
          </div>
        </header>

        <template v-if="shopSummaries.length">
          <el-table
            :data="shopSummaries"
            border
            size="small"
            highlight-current-row
            class="settlement-shop-table"
            :row-class-name="shopRowClassName"
            :span-method="shopTableSpanMethod"
            @row-click="onShopRowClick"
          >
            <el-table-column
              prop="shopName"
              :label="t('ecommerce.monthlySettlement.shopName')"
              min-width="156"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <div class="settlement-shop-name">
                  <img
                    :src="getShopIconMeta(row).src"
                    alt=""
                    class="settlement-shop-name__avatar"
                    :class="{ 'is-avatar': getShopIconMeta(row).isCustomAvatar }"
                  />
                  <span class="settlement-shop-name__text">{{ row.shopName || `#${row.shopId}` }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.monthlySettlement.colRevenue')" width="108" align="right">
              <template #default="{ row }">
                <span v-if="!isShopOrdersImported(row.shopId)" class="settlement-shop-not-imported">
                  {{ t('ecommerce.monthlySettlement.shopOrdersNotImported') }}
                </span>
                <template v-else><CnyAmount :value="row.totalRevenue" /></template>
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.monthlySettlement.colEstCost')" width="108" align="right">
              <template #default="{ row }">
                <template v-if="isShopOrdersImported(row.shopId)"><CnyAmount :value="row.estimatedTotalCost" /></template>
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.monthlySettlement.colActCost')" width="108" align="right">
              <template #default="{ row }">
                <template v-if="isShopOrdersImported(row.shopId)"><CnyAmount :value="row.actualTotalCost" /></template>
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.monthlySettlement.colEstProfit')" width="108" align="right">
              <template #default="{ row }">
                <span v-if="isShopOrdersImported(row.shopId)" class="amount-profit"><CnyAmount :value="row.estimatedTotalProfit" /></span>
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.monthlySettlement.colActProfit')" width="108" align="right">
              <template #default="{ row }">
                <span v-if="isShopOrdersImported(row.shopId)" class="amount-profit"><CnyAmount :value="row.actualTotalProfit" /></span>
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.monthlySettlement.colOrderStats')" width="130" align="center">
              <template #default="{ row }">
                <span v-if="isShopOrdersImported(row.shopId)" class="order-stats-cell">
                  <span class="is-included">{{ row.includedOrderCount ?? 0 }}</span>
                  /
                  <span class="is-excluded">{{ row.excludedOrderCount ?? 0 }}</span>
                  /
                  <span class="is-pending">{{ row.pendingOrderCount ?? 0 }}</span>
                </span>
              </template>
            </el-table-column>
          </el-table>

          <section class="shop-detail">
            <div class="shop-detail-card shop-detail-card--pending">
              <div class="shop-detail-card__head">
                <h4 class="shop-detail-card__title">{{ t('ecommerce.monthlySettlement.detailTabPending') }}</h4>
                <div v-if="selectedShop" class="shop-detail-card__shop-name settlement-shop-name">
                  <img
                    :src="getShopIconMeta(selectedShop).src"
                    alt=""
                    class="settlement-shop-name__avatar shop-detail-card__shop-avatar"
                    :class="{ 'is-avatar': getShopIconMeta(selectedShop).isCustomAvatar }"
                  />
                  <span class="settlement-shop-name__text">
                    {{ selectedShop.shopName || `#${selectedShop.shopId}` }}
                  </span>
                </div>
                <el-badge
                  v-if="selectedShop && isShopOrdersImported(selectedShop.shopId) && (selectedShop.pendingOrderCount ?? 0) > 0"
                  :value="selectedShop.pendingOrderCount"
                  class="shop-detail-card__badge"
                />
              </div>
              <div class="shop-detail-card__body shop-detail-card__body--flush">
                <el-empty
                  v-if="selectedShop && !isShopOrdersImported(selectedShop.shopId)"
                  :description="t('ecommerce.monthlySettlement.shopOrdersNotImportedPending')"
                />
                <template v-else-if="selectedShop?.pendingOrders?.length">
                  <el-table :data="selectedShop.pendingOrders" border size="small">
                    <el-table-column
                      prop="platformOrderNo"
                      :label="t('ecommerce.salesOrder.platformOrderNo')"
                      min-width="150"
                      show-overflow-tooltip
                    />
                    <el-table-column prop="status" :label="t('ecommerce.salesOrder.status')" width="110">
                      <template #default="{ row }">{{ statusLabel(row.status) }}</template>
                    </el-table-column>
                    <el-table-column
                      prop="productName"
                      :label="t('ecommerce.inventory.productName')"
                      min-width="140"
                      show-overflow-tooltip
                    >
                      <template #default="{ row }">{{ row.productName || '—' }}</template>
                    </el-table-column>
                    <el-table-column
                      prop="skuName"
                      :label="t('ecommerce.salesOrder.skuSpecName')"
                      min-width="120"
                      show-overflow-tooltip
                    >
                      <template #default="{ row }">{{ row.skuName || '—' }}</template>
                    </el-table-column>
                    <el-table-column :label="t('ecommerce.salesOrder.receivedAmount')" width="100" align="right">
                      <template #default="{ row }"><CnyAmount :value="row.receivedAmount" /></template>
                    </el-table-column>
                    <el-table-column :label="t('ecommerce.salesOrder.orderTime')" width="160">
                      <template #default="{ row }">{{ formatDateTime(row.orderTime) }}</template>
                    </el-table-column>
                    <el-table-column :label="t('ecommerce.monthlySettlement.includeDecision')" width="200" fixed="right">
                      <template #default="{ row }">
                        <el-radio-group
                          :model-value="resolvePendingDecision(row)"
                          size="small"
                          class="pending-decision-radio"
                          @update:model-value="(v: boolean) => onPendingDecisionChange(row.orderId!, v)"
                        >
                          <el-radio class="is-include" :value="true">{{ t('ecommerce.monthlySettlement.include') }}</el-radio>
                          <el-radio class="is-exclude" :value="false">{{ t('ecommerce.monthlySettlement.exclude') }}</el-radio>
                        </el-radio-group>
                      </template>
                    </el-table-column>
                  </el-table>
                  <div class="pending-actions">
                    <el-button
                      plain
                      size="small"
                      class="pending-actions__include-all"
                      @click="setAllPendingDecisions(selectedShop, true)"
                    >
                      {{ t('ecommerce.monthlySettlement.includeAll') }}
                    </el-button>
                    <el-button
                      plain
                      size="small"
                      class="pending-actions__exclude-all"
                      @click="setAllPendingDecisions(selectedShop, false)"
                    >
                      {{ t('ecommerce.monthlySettlement.excludeAll') }}
                    </el-button>
                    <el-button
                      type="primary"
                      class="pending-actions__confirm"
                      :loading="savingDecisions"
                      @click="savePendingDecisions(selectedShop)"
                    >
                      {{ t('ecommerce.monthlySettlement.confirmDecisions') }}
                    </el-button>
                  </div>
                </template>
                <el-empty
                  v-else-if="selectedShop"
                  :description="t('ecommerce.monthlySettlement.pendingOrdersDone')"
                />
                <el-empty v-else :description="t('ecommerce.monthlySettlement.pendingSelectShopHint')" />
              </div>
            </div>

            <div class="shop-detail__grid">
              <div class="shop-detail-card shop-detail-card--summary">
                <div class="shop-detail-card__head">
                  <h4 class="shop-detail-card__title">{{ t('ecommerce.monthlySettlement.detailTabSummary') }}</h4>
                  <span class="shop-detail-card__scope">{{ t('ecommerce.monthlySettlement.overallSummaryScope') }}</span>
                </div>
                <div class="shop-detail-card__body shop-detail-card__body--flush">
                  <el-descriptions :column="1" border size="small">
                    <el-descriptions-item :label="t('ecommerce.monthlySettlement.totalRevenue')">
                      <span class="amount-primary"><CnyAmount :value="overallSummary.totalRevenue" /></span>
                    </el-descriptions-item>
                    <el-descriptions-item :label="t('ecommerce.monthlySettlement.estimatedTotalCost')">
                      <CnyAmount :value="overallSummary.estimatedTotalCost" />
                    </el-descriptions-item>
                    <el-descriptions-item :label="t('ecommerce.monthlySettlement.actualTotalCost')">
                      <CnyAmount :value="overallSummary.actualTotalCost" />
                    </el-descriptions-item>
                    <el-descriptions-item :label="t('ecommerce.monthlySettlement.estimatedTotalProfit')">
                      <span class="amount-profit"><CnyAmount :value="overallSummary.estimatedTotalProfit" /></span>
                    </el-descriptions-item>
                    <el-descriptions-item :label="t('ecommerce.monthlySettlement.actualTotalProfit')">
                      <span class="amount-profit"><CnyAmount :value="overallSummary.actualTotalProfit" /></span>
                    </el-descriptions-item>
                    <el-descriptions-item :label="t('ecommerce.monthlySettlement.orderStats')">
                      {{ t('ecommerce.monthlySettlement.includedCount', { count: overallSummary.includedOrderCount }) }}
                      /
                      {{ t('ecommerce.monthlySettlement.excludedCount', { count: overallSummary.excludedOrderCount }) }}
                      /
                      {{ t('ecommerce.monthlySettlement.pendingCount', { count: overallSummary.pendingOrderCount }) }}
                    </el-descriptions-item>
                  </el-descriptions>
                </div>
              </div>

              <div class="shop-detail-card shop-detail-card--max-profit">
                <div class="shop-detail-card__head">
                  <h4 class="shop-detail-card__title">
                    <span class="max-profit-card__crown" aria-hidden="true">👑</span>
                    {{ t('ecommerce.monthlySettlement.detailTabMaxProfit') }}
                  </h4>
                  <el-button
                    size="small"
                    :type="maxProfitShowAll ? 'primary' : 'default'"
                    plain
                    class="shop-detail-card__all-shops-btn"
                    @click="maxProfitShowAll = true"
                  >
                    {{ t('ecommerce.monthlySettlement.allShops') }}
                  </el-button>
                </div>
                <div class="shop-detail-card__body">
                  <div v-if="displayedMaxProfit?.platformOrderNo || displayedMaxProfit?.orderNo" class="max-profit-card">
                    <div class="max-profit-card__overview">
                      <div class="max-profit-card__thumb">
                        <el-image
                          v-if="displayedMaxProfit.productImageUrl"
                          :src="displayedMaxProfit.productImageUrl"
                          fit="cover"
                          class="max-profit-card__image"
                        >
                          <template #error>
                            <div class="max-profit-card__thumb-fallback">
                              <el-icon><Goods /></el-icon>
                            </div>
                          </template>
                        </el-image>
                        <div v-else class="max-profit-card__thumb-fallback">
                          <el-icon><Goods /></el-icon>
                        </div>
                      </div>
                      <dl class="max-profit-card__facts">
                        <div class="max-profit-card__fact max-profit-card__fact--order-no">
                          <dd>
                            <button
                              v-if="displayedMaxProfit.orderId"
                              type="button"
                              class="max-profit-card__order-link"
                              @click="goMaxProfitOrderDetail"
                            >
                              {{ displayedMaxProfit.platformOrderNo || displayedMaxProfit.orderNo }}
                            </button>
                            <span v-else class="max-profit-card__order-no">
                              {{ displayedMaxProfit.platformOrderNo || displayedMaxProfit.orderNo }}
                            </span>
                          </dd>
                        </div>
                        <div v-if="displayedMaxProfit.shopName" class="max-profit-card__fact max-profit-card__fact--shop">
                          <dd class="max-profit-card__shop-line">
                            <img
                              :src="getMaxProfitShopIcon().src"
                              alt=""
                              class="max-profit-card__shop-avatar"
                              :class="{ 'is-avatar': getMaxProfitShopIcon().isCustomAvatar }"
                            />
                            <span>{{ displayedMaxProfit.shopName }}</span>
                          </dd>
                        </div>
                        <div class="max-profit-card__fact">
                          <dt>{{ t('ecommerce.inventory.productName') }}</dt>
                          <dd>{{ displayedMaxProfit.productName || '—' }}</dd>
                        </div>
                        <div class="max-profit-card__fact">
                          <dt>{{ t('ecommerce.monthlySettlement.maxProfitSkuName') }}</dt>
                          <dd>{{ displayedMaxProfit.skuName || '—' }}</dd>
                        </div>
                        <div class="max-profit-card__fact">
                          <dt>{{ t('ecommerce.monthlySettlement.maxProfitOrderTime') }}</dt>
                          <dd>{{ displayedMaxProfit.orderTime ? formatDateTime(displayedMaxProfit.orderTime) : '—' }}</dd>
                        </div>
                      </dl>
                    </div>

                    <div class="max-profit-card__rows">
                      <div class="max-profit-card__row">
                        <span>{{ t('ecommerce.monthlySettlement.maxProfitSalesAmount') }}</span>
                        <strong><CnyAmount :value="displayedMaxProfit.receivedAmount" /></strong>
                      </div>
                      <div class="max-profit-card__row">
                        <span>{{ t('ecommerce.monthlySettlement.maxProfitEstimatedCost') }}</span>
                        <strong><CnyAmount :value="displayedMaxProfit.estimatedCostAmount" /></strong>
                      </div>
                      <div class="max-profit-card__row">
                        <span>{{ t('ecommerce.monthlySettlement.maxProfitActualCost') }}</span>
                        <strong><CnyAmount :value="displayedMaxProfit.actualCostAmount" /></strong>
                      </div>
                      <div class="max-profit-card__row">
                        <span>{{ t('ecommerce.monthlySettlement.maxProfitEstimatedProfit') }}</span>
                        <strong class="is-profit"><CnyAmount :value="displayedMaxProfit.profitAmount" /></strong>
                      </div>
                    </div>

                    <div class="max-profit-card__hero">
                      <span class="max-profit-card__hero-label">
                        {{ t('ecommerce.monthlySettlement.maxProfitActualProfit') }}
                      </span>
                      <strong
                        class="max-profit-card__hero-value"
                        :class="{ 'is-unknown': maxProfitActualProfitDisplay.unknown }"
                      >
                        {{ maxProfitActualProfitDisplay.text }}
                      </strong>
                    </div>
                  </div>
                  <el-empty v-else class="max-profit-card__empty" :description="t('ecommerce.monthlySettlement.maxProfitEmpty')" />
                </div>
              </div>
            </div>
          </section>
        </template>

        <el-empty
          v-else-if="!calculating && calculated"
          :description="t('ecommerce.monthlySettlement.noData')"
        />
        <el-empty
          v-else-if="!calculating && !calculated"
          :description="t('ecommerce.monthlySettlement.notCalculatedHint')"
        />
      </main>
    </div>

    <el-dialog
      v-model="buyerExcludeVisible"
      :title="t('ecommerce.monthlySettlement.buyerExcludeConfig')"
      width="720px"
      class="buyer-exclude-dialog"
      destroy-on-close
      @closed="onBuyerExcludeDialogClosed"
    >
      <section class="buyer-exclude-panel">
        <header class="buyer-exclude-panel__head">
          <p class="buyer-exclude-panel__hint">{{ t('ecommerce.monthlySettlement.buyerExcludeDialogHint') }}</p>
          <div class="buyer-exclude-panel__stats">
            <div class="buyer-exclude-stat">
              <span class="buyer-exclude-stat__label">{{ t('ecommerce.monthlySettlement.buyerExcludeTotalCount') }}</span>
              <strong class="buyer-exclude-stat__value">{{ buyerExcludeStats.total }}</strong>
            </div>
            <div class="buyer-exclude-stat buyer-exclude-stat--primary">
              <span class="buyer-exclude-stat__label">{{ t('ecommerce.monthlySettlement.buyerExcludeGlobalCount') }}</span>
              <strong class="buyer-exclude-stat__value">{{ buyerExcludeStats.globalCount }}</strong>
            </div>
            <div class="buyer-exclude-stat buyer-exclude-stat--warning">
              <span class="buyer-exclude-stat__label">{{ t('ecommerce.monthlySettlement.buyerExcludeShopCount') }}</span>
              <strong class="buyer-exclude-stat__value">{{ buyerExcludeStats.shopCount }}</strong>
            </div>
          </div>
        </header>

        <section class="buyer-exclude-form-card">
          <h4 class="buyer-exclude-form-card__title">{{ t('ecommerce.monthlySettlement.buyerExcludeDialogAddTitle') }}</h4>
          <div class="buyer-exclude-form">
            <label class="buyer-exclude-field">
              <span class="buyer-exclude-field__label">{{ t('ecommerce.salesOrder.shop') }}</span>
              <el-select
                v-model="excludeFormShopId"
                clearable
                filterable
                :placeholder="t('ecommerce.monthlySettlement.buyerExcludeGlobalShopHint')"
                class="buyer-exclude-field__control"
              >
                <el-option v-for="s in shopOptions" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </label>
            <label class="buyer-exclude-field buyer-exclude-field--grow">
              <span class="buyer-exclude-field__label">{{ t('ecommerce.monthlySettlement.buyerName') }}</span>
              <el-input
                v-model="excludeFormBuyerName"
                :placeholder="t('ecommerce.monthlySettlement.buyerNamePlaceholder')"
                class="buyer-exclude-field__control"
                @keyup.enter="addBuyerExclude"
              />
            </label>
            <label class="buyer-exclude-field buyer-exclude-field--grow">
              <span class="buyer-exclude-field__label">{{ t('ecommerce.monthlySettlement.remark') }}</span>
              <el-input
                v-model="excludeFormRemark"
                :placeholder="t('ecommerce.monthlySettlement.remarkPlaceholder')"
                class="buyer-exclude-field__control"
                @keyup.enter="addBuyerExclude"
              />
            </label>
            <el-button
              type="primary"
              class="buyer-exclude-form__submit"
              :loading="savingBuyerExclude"
              @click="addBuyerExclude"
            >
              {{ t('ecommerce.monthlySettlement.add') }}
            </el-button>
          </div>
        </section>

        <section class="buyer-exclude-list-section">
          <div class="buyer-exclude-list__toolbar">
            <h4 class="buyer-exclude-list__title">{{ t('ecommerce.monthlySettlement.buyerExcludeDialogListTitle') }}</h4>
            <el-input
              v-model="excludeSearchKeyword"
              clearable
              :prefix-icon="Search"
              :placeholder="t('ecommerce.monthlySettlement.buyerExcludeSearchPlaceholder')"
              class="buyer-exclude-list__search"
            />
          </div>

          <el-empty
            v-if="!loadingExcludes && !filteredBuyerExcludes.length"
            class="buyer-exclude-list__empty"
            :image-size="72"
          >
            <template #description>
              <p class="buyer-exclude-list__empty-title">{{ t('ecommerce.monthlySettlement.buyerExcludeEmptyTitle') }}</p>
              <p class="buyer-exclude-list__empty-desc">{{ t('ecommerce.monthlySettlement.buyerExcludeEmptyDesc') }}</p>
            </template>
          </el-empty>

          <div v-else v-loading="loadingExcludes" class="buyer-exclude-list">
            <article
              v-for="item in filteredBuyerExcludes"
              :key="item.id"
              class="buyer-exclude-card"
            >
              <div class="buyer-exclude-card__shop">
                <img
                  :src="getExcludeShopIconMeta(item).src"
                  alt=""
                  class="buyer-exclude-card__avatar"
                  :class="{ 'is-custom': getExcludeShopIconMeta(item).isCustomAvatar }"
                />
                <span class="buyer-exclude-card__shop-name">
                  {{ item.shopName || t('ecommerce.monthlySettlement.allShops') }}
                </span>
              </div>
              <div class="buyer-exclude-card__main">
                <div class="buyer-exclude-card__buyer">{{ item.buyerName }}</div>
                <div v-if="item.remark" class="buyer-exclude-card__remark">{{ item.remark }}</div>
                <div v-if="item.createTime" class="buyer-exclude-card__time">{{ formatExcludeTime(item.createTime) }}</div>
              </div>
              <el-button
                link
                type="danger"
                class="buyer-exclude-card__remove"
                :title="t('ecommerce.monthlySettlement.delete')"
                @click="removeBuyerExclude(item.id!)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </article>
          </div>
        </section>
      </section>
    </el-dialog>

    <ExpressBillImportDialog
      v-model="expressBillVisible"
      :initial-month="settlementMonth"
      lock-month
      @imported="onExpressBillImported"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Check, Warning, ArrowRight, Goods, Delete, Search } from '@element-plus/icons-vue'
import { fetchShopOptions, type EcShop } from '@/api/ecommerce/shop'
import { resolveShopIconMeta } from '@/utils/shopVisual'
import { fetchSalesOrderMonthlyOverview, type EcSalesOrderMonthlyOverview, type ShopImportStatus } from '@/api/ecommerce/salesOrder'
import { ecommercePathForModule } from '@/data/ecommerce-nav'
import ExpressBillImportDialog from '@/views/ecommerce/ExpressBillImportDialog.vue'
import MonthStepper from '@/components/ecommerce/MonthStepper.vue'
import CnyAmount from '@/components/CnyAmount.vue'
import { formatMoney } from '@/utils/formatMoney'
import {
  deleteSettlementBuyerExclude,
  fetchExpressBillImported,
  fetchExpressBillRecords,
  calculateMonthlySettlement,
  fetchMonthlySettlementSnapshot,
  fetchSettlementBuyerExcludes,
  saveSettlementBuyerExclude,
  saveSettlementOrderDecisions,
  type ExpressBillRecord,
  type MonthlySettlementShopSummary,
  type SettlementBuyerExclude,
} from '@/api/ecommerce/monthlySettlement'
import { fetchExpressStations, type EcExpressStation } from '@/api/ecommerce/express'
import { resolveExpressIconMetaFromStation } from '@/utils/expressVisual'
import { formatDateTime, defaultOrderMonth } from '@/utils/date'

interface PrepExpressBillCard {
  id: number
  name: string
  iconSrc: string
  isCustomAvatar: boolean
  matched: number
  total: number
  gapCount?: number
}

type PrepTone = 'success' | 'warning' | 'danger' | 'muted'
type PrepStatusTagType = 'success' | 'warning' | 'danger' | 'info'

interface PrepTask {
  key: string
  title: string
  desc: string
  descHighlight?: string
  tone: PrepTone
  statusTag?: { label: string; type: PrepStatusTagType }
  lastOperationTime?: string
  lastTimeLabelKey?: string
  expressBillCards?: PrepExpressBillCard[]
  subItems?: string[]
  action?: () => void
}

const { t } = useI18n()
const router = useRouter()

const settlementMonth = ref(defaultOrderMonth())
const shopOptions = ref<EcShop[]>([])
const calculating = ref(false)
const submitting = ref(false)
const prepLoading = ref(false)
const calculated = ref(false)
const savingDecisions = ref(false)
const expressBillImported = ref(false)
const result = ref<{ shops: MonthlySettlementShopSummary[]; expressBillImported?: boolean } | null>(null)
const pendingDecisions = reactive<Record<number, boolean>>({})
const selectedShopId = ref<number | null>(null)
const maxProfitShowAll = ref(false)

const globalMaxProfit = computed(() => {
  let bestShop: MonthlySettlementShopSummary | null = null
  let bestOrder: NonNullable<MonthlySettlementShopSummary['maxProfitOrder']> | null = null
  for (const shop of shopSummaries.value) {
    const order = shop.maxProfitOrder
    if (!order?.orderNo && !order?.platformOrderNo) continue
    const profit = order.profitAmount ?? Number.NEGATIVE_INFINITY
    if (!bestOrder || profit > (bestOrder.profitAmount ?? Number.NEGATIVE_INFINITY)) {
      bestShop = shop
      bestOrder = order
    }
  }
  if (!bestShop || !bestOrder) return null
  return {
    ...bestOrder,
    shopId: bestShop.shopId,
    shopName: bestShop.shopName || `#${bestShop.shopId}`,
  }
})

const displayedMaxProfit = computed(() => {
  if (maxProfitShowAll.value) {
    return globalMaxProfit.value
  }
  const shop = selectedShop.value
  const order = shop?.maxProfitOrder
  if (!order?.orderNo && !order?.platformOrderNo) return null
  return {
    ...order,
    shopId: shop.shopId,
    shopName: shop.shopName || `#${shop.shopId}`,
  }
})

const maxProfitActualProfitDisplay = computed(() => {
  const item = displayedMaxProfit.value
  if (!item) return { text: '—', unknown: false }

  const reasonCode = item.actualProfitUnknownReason
  if (reasonCode) {
    return {
      text: t(`ecommerce.monthlySettlement.maxProfitUnknownReason.${reasonCode}`),
      unknown: true,
    }
  }

  if (item.actualProfitAmount != null) {
    return { text: formatMoney(item.actualProfitAmount), unknown: false }
  }

  if (!expressBillImported.value) {
    return {
      text: t('ecommerce.monthlySettlement.maxProfitUnknownReason.EXPRESS_BILL_NOT_IMPORTED'),
      unknown: true,
    }
  }
  return {
    text: t('ecommerce.monthlySettlement.maxProfitUnknownReason.ACTUAL_FREIGHT_MISSING'),
    unknown: true,
  }
})

const orderOverview = ref<EcSalesOrderMonthlyOverview | null>(null)
const expressBillRecords = ref<ExpressBillRecord[]>([])
const expressStations = ref<EcExpressStation[]>([])
const buyerExcludeCount = ref(0)
const buyerExcludesSnapshot = ref<SettlementBuyerExclude[]>([])
const lastBuyerExcludeOpAt = ref<string | null>(null)
const lastPendingDecisionAt = ref<string | null>(null)
const lastCalculatedAt = ref<string | null>(null)
let prepRequestSeq = 0
let prepLoadingCount = 0
let calculatingCount = 0
let submittingCount = 0
let enterPromise: Promise<void> | null = null
let pageLoadPromise: Promise<void> | null = null
let bootstrapped = false
const ignoreMonthWatch = ref(true)

const buyerExcludeVisible = ref(false)
const expressBillVisible = ref(false)
const loadingExcludes = ref(false)
const savingBuyerExclude = ref(false)
const buyerExcludes = ref<SettlementBuyerExclude[]>([])
const excludeFormShopId = ref<number | undefined>()
const excludeFormBuyerName = ref('')
const excludeFormRemark = ref('')
const excludeSearchKeyword = ref('')

const shopSummaries = computed(() => result.value?.shops ?? [])

const shopOptionMap = computed(() => {
  const map = new Map<number, EcShop>()
  for (const shop of shopOptions.value) {
    map.set(shop.id, shop)
  }
  return map
})

function getShopIconMeta(row: MonthlySettlementShopSummary) {
  const shop = shopOptionMap.value.get(row.shopId)
  return resolveShopIconMeta(
    row.shopName ?? shop?.name,
    shop?.platformName,
    shop?.platformCode,
    shop?.avatarUrl,
  )
}

function getMaxProfitShopIcon() {
  const item = displayedMaxProfit.value
  if (!item?.shopId) {
    return resolveShopIconMeta(item?.shopName)
  }
  const shop = shopOptionMap.value.get(item.shopId)
  return resolveShopIconMeta(
    item.shopName ?? shop?.name,
    shop?.platformName,
    shop?.platformCode,
    shop?.avatarUrl,
  )
}

function getExcludeShopIconMeta(item: SettlementBuyerExclude) {
  if (!item.shopId) {
    return resolveShopIconMeta(item.shopName ?? undefined)
  }
  const shop = shopOptionMap.value.get(item.shopId)
  return resolveShopIconMeta(
    item.shopName ?? shop?.name,
    shop?.platformName,
    shop?.platformCode,
    shop?.avatarUrl,
  )
}

const buyerExcludeStats = computed(() => {
  const list = buyerExcludes.value
  return {
    total: list.length,
    globalCount: list.filter((item) => !item.shopId).length,
    shopCount: list.filter((item) => item.shopId).length,
  }
})

const filteredBuyerExcludes = computed(() => {
  const keyword = excludeSearchKeyword.value.trim().toLowerCase()
  if (!keyword) return buyerExcludes.value
  return buyerExcludes.value.filter((item) => {
    const shop = (item.shopName || t('ecommerce.monthlySettlement.allShops')).toLowerCase()
    const buyer = item.buyerName.toLowerCase()
    const remark = (item.remark || '').toLowerCase()
    return shop.includes(keyword) || buyer.includes(keyword) || remark.includes(keyword)
  })
})

function formatExcludeTime(value?: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 19)
}

function onBuyerExcludeDialogClosed() {
  excludeFormShopId.value = undefined
  excludeFormBuyerName.value = ''
  excludeFormRemark.value = ''
  excludeSearchKeyword.value = ''
}

const shopImportStatusMap = computed(() => {
  const map = new Map<number, ShopImportStatus>()
  for (const shop of orderOverview.value?.shops ?? []) {
    map.set(shop.shopId, shop.status)
  }
  return map
})

function isShopOrdersImported(shopId: number) {
  const status = shopImportStatusMap.value.get(shopId)
  return status != null && status !== 'NOT_IMPORTED'
}

function shopTableSpanMethod({
  row,
  columnIndex,
}: {
  row: MonthlySettlementShopSummary
  columnIndex: number
}) {
  if (!isShopOrdersImported(row.shopId)) {
    if (columnIndex === 1) {
      return { rowspan: 1, colspan: 6 }
    }
    if (columnIndex > 1) {
      return { rowspan: 0, colspan: 0 }
    }
  }
  return { rowspan: 1, colspan: 1 }
}

const settlementPeriodLabel = computed(() => {
  const month = settlementMonth.value
  if (!month) return ''
  const [y, m] = month.split('-')
  if (!y || !m) return month
  return `${y}年${m}月`
})

const lastCalculatedDisplay = computed(() => {
  if (!lastCalculatedAt.value) return ''
  const formatted = formatDateTime(lastCalculatedAt.value)
  return formatted === '—' ? '' : formatted
})

const selectedShop = computed(() => {
  if (selectedShopId.value == null) return null
  return shopSummaries.value.find((s) => s.shopId === selectedShopId.value) ?? null
})

function sumShopMetric(
  shops: MonthlySettlementShopSummary[],
  pick: (shop: MonthlySettlementShopSummary) => number | null | undefined,
) {
  return shops.reduce((sum, shop) => sum + (pick(shop) ?? 0), 0)
}

const overallSummary = computed(() => {
  const shops = shopSummaries.value.filter((shop) => isShopOrdersImported(shop.shopId))
  return {
    totalRevenue: sumShopMetric(shops, (s) => s.totalRevenue),
    estimatedTotalCost: sumShopMetric(shops, (s) => s.estimatedTotalCost),
    actualTotalCost: sumShopMetric(shops, (s) => s.actualTotalCost),
    estimatedTotalProfit: sumShopMetric(shops, (s) => s.estimatedTotalProfit),
    actualTotalProfit: sumShopMetric(shops, (s) => s.actualTotalProfit),
    includedOrderCount: sumShopMetric(shops, (s) => s.includedOrderCount),
    excludedOrderCount: sumShopMetric(shops, (s) => s.excludedOrderCount),
    pendingOrderCount: sumShopMetric(shops, (s) => s.pendingOrderCount),
  }
})

const totalPendingOrders = computed(() =>
  shopSummaries.value.reduce((sum, shop) => sum + (shop.pendingOrderCount ?? 0), 0),
)

const salesOrdersImported = computed(() => {
  const overview = orderOverview.value
  const orderCount = overview?.totalOrderCount ?? 0
  const reviewCount = overview?.pendingReviewCount ?? 0
  return orderCount > 0 || reviewCount > 0
})

const calculateButtonDisabled = computed(() => !salesOrdersImported.value)

const calculateButtonLabel = computed(() => {
  if (calculated.value) {
    return t('ecommerce.monthlySettlement.recalculate')
  }
  if (salesOrdersImported.value && !expressBillImported.value) {
    return t('ecommerce.monthlySettlement.preCalculate')
  }
  return t('ecommerce.monthlySettlement.calculate')
})

const calculateButtonMode = computed(() => {
  if (!salesOrdersImported.value) return 'disabled'
  if (calculated.value) return 'recalculate'
  if (!expressBillImported.value) return 'precalculate'
  return 'calculate'
})

const calculateButtonTooltip = computed(() => {
  if (
    calculateButtonDisabled.value
    || calculated.value
    || expressBillImported.value
    || !salesOrdersImported.value
  ) {
    return ''
  }
  return t('ecommerce.monthlySettlement.preCalculateTip')
})

const expressStationMap = computed(() => {
  const map = new Map<number, EcExpressStation>()
  for (const station of expressStations.value) {
    map.set(station.id, station)
  }
  return map
})

function stationRecordKey(record: ExpressBillRecord): string {
  if (record.otherExpress) return 'other'
  return String(record.expressStationId ?? record.expressStationName ?? record.id)
}

function filterExpressBillRecordsByMonth(records: ExpressBillRecord[], month: string) {
  const monthKey = month.trim()
  return records.filter((record) => (record.billMonth ?? '').trim() === monthKey)
}

function buildExpressBillCards(records: ExpressBillRecord[], month: string): PrepExpressBillCard[] {
  const scopedRecords = filterExpressBillRecordsByMonth(records, month)
  const grouped = new Map<string, ExpressBillRecord[]>()
  for (const record of scopedRecords) {
    const key = stationRecordKey(record)
    const list = grouped.get(key) ?? []
    list.push(record)
    grouped.set(key, list)
  }

  const cards: PrepExpressBillCard[] = []
  for (const stationRecords of grouped.values()) {
    // API 已按导入时间倒序；优先展示最近一次有账单行数的文件导入，避免手动补录批次 (0/0) 占位
    const primary =
      stationRecords.find((r) => (r.totalRows ?? 0) > 0) ?? stationRecords[0]
    const station = primary.expressStationId
      ? expressStationMap.value.get(primary.expressStationId)
      : undefined
    const name = primary.expressStationName || station?.name || '—'
    const iconMeta = resolveExpressIconMetaFromStation(
      station ?? { name: primary.expressStationName ?? undefined },
    )
    const gapCount = stationRecords.reduce((max, r) => Math.max(max, r.gapOrderRows ?? 0), 0)
    cards.push({
      id: primary.id,
      name,
      iconSrc: iconMeta.src,
      isCustomAvatar: iconMeta.isCustomAvatar,
      matched: primary.matchedRows ?? 0,
      total: primary.totalRows ?? 0,
      gapCount: gapCount > 0 ? gapCount : undefined,
    })
  }
  return cards
}

const statusOptions = computed(() => [
  { value: 'DRAFT', label: t('ecommerce.salesOrder.statusDraft') },
  { value: 'PAID', label: t('ecommerce.salesOrder.statusPaid') },
  { value: 'PARTIAL_SHIPPED', label: t('ecommerce.salesOrder.statusPartialShipped') },
  { value: 'SHIPPED', label: t('ecommerce.salesOrder.statusShipped') },
  { value: 'PARTIAL_REFUND', label: t('ecommerce.salesOrder.statusPartialRefund') },
  { value: 'COMPLETED', label: t('ecommerce.salesOrder.statusCompleted') },
  { value: 'REFUNDED', label: t('ecommerce.salesOrder.statusRefunded') },
  { value: 'CANCELLED', label: t('ecommerce.salesOrder.statusCancelled') },
])

const prepTasks = computed<PrepTask[]>(() => {
  const overview = orderOverview.value
  const orderCount = overview?.totalOrderCount ?? 0
  const shopCount = overview?.importedShopCount ?? 0
  const reviewCount = overview?.pendingReviewCount ?? 0

  let orderTone: PrepTone = 'danger'
  let orderStatusTag: PrepTask['statusTag'] = {
    label: t('ecommerce.monthlySettlement.prepStatusNotImported'),
    type: 'danger',
  }
  let orderDesc = ''
  let orderDescHighlight = ''
  if (reviewCount > 0) {
    orderTone = 'warning'
    orderStatusTag = {
      label: t('ecommerce.monthlySettlement.prepStatusPendingReview'),
      type: 'warning',
    }
    orderDesc = t('ecommerce.monthlySettlement.salesOrdersImportedPrefix')
    orderDescHighlight = t('ecommerce.monthlySettlement.salesOrdersImportedCount', { shopCount, orderCount })
  } else if (orderCount > 0) {
    orderTone = 'success'
    orderStatusTag = {
      label: t('ecommerce.monthlySettlement.prepStatusCompleted'),
      type: 'success',
    }
    orderDesc = t('ecommerce.monthlySettlement.salesOrdersImportedPrefix')
    orderDescHighlight = t('ecommerce.monthlySettlement.salesOrdersImportedCount', { shopCount, orderCount })
  } else {
    orderDesc = t('ecommerce.monthlySettlement.salesOrdersNotImportedHint')
  }

  const expressBillCards: PrepExpressBillCard[] = []
  let expressTone: PrepTone = expressBillImported.value ? 'success' : 'danger'
  let expressStatusTag: PrepTask['statusTag'] = expressBillImported.value
    ? { label: t('ecommerce.monthlySettlement.prepStatusCompleted'), type: 'success' }
    : { label: t('ecommerce.monthlySettlement.prepStatusNotImported'), type: 'danger' }
  let expressDesc = ''
  let expressHasGap = false
  let expressHasUnmatched = false

  const monthRecords = filterExpressBillRecordsByMonth(expressBillRecords.value, settlementMonth.value)
  if (monthRecords.length) {
    for (const record of monthRecords) {
      if ((record.gapOrderRows ?? 0) > 0) {
        expressHasGap = true
      }
      if ((record.unmatchedRows ?? 0) > 0) {
        expressHasUnmatched = true
      }
    }
    expressBillCards.push(...buildExpressBillCards(monthRecords, settlementMonth.value))
    if (expressHasGap || expressHasUnmatched) {
      expressTone = 'warning'
      expressStatusTag = {
        label: t('ecommerce.monthlySettlement.prepStatusPendingFill'),
        type: 'warning',
      }
    } else if (expressBillImported.value) {
      expressTone = 'success'
      expressStatusTag = {
        label: t('ecommerce.monthlySettlement.prepStatusCompleted'),
        type: 'success',
      }
    }
    const totalMatched = expressBillCards.reduce((sum, card) => sum + card.matched, 0)
    expressDesc = t('ecommerce.monthlySettlement.expressBillImportedSummary', { count: totalMatched })
  } else if (!expressBillImported.value) {
    expressDesc = t('ecommerce.monthlySettlement.expressBillImportPrompt')
  }

  const excludeTone: PrepTone = buyerExcludeCount.value > 0 ? 'success' : 'muted'
  const excludeStatusTag: PrepTask['statusTag'] = buyerExcludeCount.value > 0
    ? { label: t('ecommerce.monthlySettlement.prepStatusConfigured'), type: 'success' }
    : { label: t('ecommerce.monthlySettlement.prepStatusNotConfigured'), type: 'info' }
  const excludeDesc =
    buyerExcludeCount.value > 0
      ? t('ecommerce.monthlySettlement.buyerExcludePrefix')
      : t('ecommerce.monthlySettlement.buyerExcludePurposeHint')
  const excludeDescHighlight =
    buyerExcludeCount.value > 0
      ? t('ecommerce.monthlySettlement.buyerExcludeCountHighlight', { count: buyerExcludeCount.value })
      : ''

  const pendingCount = totalPendingOrders.value
  const salesOrdersImported = orderCount > 0 || reviewCount > 0
  let pendingTone: PrepTone = 'muted'
  let pendingStatusTag: PrepTask['statusTag'] | undefined
  let pendingDesc = ''
  let pendingDescHighlight = ''
  let pendingAction: (() => void) | undefined = goReviewPending
  if (!salesOrdersImported) {
    pendingTone = 'muted'
    pendingStatusTag = {
      label: t('ecommerce.monthlySettlement.prepStatusPendingImport'),
      type: 'info',
    }
    pendingDesc = t('ecommerce.monthlySettlement.pendingOrdersImportFirstHint')
    pendingAction = goImportOrders
  } else if (!calculated.value) {
    pendingTone = 'muted'
    pendingStatusTag = {
      label: t('ecommerce.monthlySettlement.prepStatusPendingCalculate'),
      type: 'info',
    }
  } else if (pendingCount > 0) {
    pendingTone = 'warning'
    pendingStatusTag = {
      label: t('ecommerce.monthlySettlement.prepStatusPendingDecision'),
      type: 'warning',
    }
    pendingDesc = t('ecommerce.monthlySettlement.pendingOrdersPrefix')
    pendingDescHighlight = t('ecommerce.monthlySettlement.pendingOrdersCountHighlight', { count: pendingCount })
  } else if (shopSummaries.value.length > 0) {
    pendingTone = 'success'
    pendingStatusTag = {
      label: t('ecommerce.monthlySettlement.prepStatusCompleted'),
      type: 'success',
    }
  } else {
    pendingStatusTag = {
      label: t('ecommerce.monthlySettlement.prepStatusPendingCalculate'),
      type: 'info',
    }
  }

  const orderLastTime =
    orderCount > 0 || reviewCount > 0
      ? pickLatestPrepTime(
          overview?.lastImportTime,
          ...(overview?.shops?.map((s) => s.lastImportTime) ?? []),
        )
      : undefined
  const expressLastTime = pickLatestPrepTime(...expressBillRecords.value.map((r) => r.createTime))
  const excludeLastTime = pickLatestPrepTime(
    ...buyerExcludesSnapshot.value.map((item) => item.createTime),
    lastBuyerExcludeOpAt.value,
  )
  const pendingLastTime = pickLatestPrepTime(
    lastPendingDecisionAt.value,
    calculated.value ? lastCalculatedAt.value : null,
  )

  return [
    {
      key: 'orders',
      title: t('ecommerce.monthlySettlement.prepSalesOrders'),
      desc: orderDesc,
      descHighlight: orderDescHighlight || undefined,
      tone: orderTone,
      statusTag: orderStatusTag,
      lastOperationTime: orderLastTime,
      lastTimeLabelKey: 'ecommerce.monthlySettlement.prepLastImport',
      action: goImportOrders,
    },
    {
      key: 'express',
      title: t('ecommerce.monthlySettlement.prepExpressBill'),
      desc: expressDesc,
      tone: expressTone,
      statusTag: expressStatusTag,
      lastOperationTime: expressLastTime,
      expressBillCards: expressBillCards.length ? expressBillCards : undefined,
      action: openExpressBillDialog,
    },
    {
      key: 'exclude',
      title: t('ecommerce.monthlySettlement.prepBuyerExclude'),
      desc: excludeDesc,
      descHighlight: excludeDescHighlight || undefined,
      tone: excludeTone,
      statusTag: excludeStatusTag,
      lastOperationTime: excludeLastTime,
      action: () => {
        buyerExcludeVisible.value = true
      },
    },
    {
      key: 'pending',
      title: t('ecommerce.monthlySettlement.prepPendingOrders'),
      desc: pendingDesc,
      descHighlight: pendingDescHighlight || undefined,
      tone: pendingTone,
      statusTag: pendingStatusTag,
      lastOperationTime: pendingLastTime,
      action: pendingAction,
    },
  ]
})

function formatPrepLastTime(value?: string | Date | null) {
  if (value == null || value === '') return undefined
  const formatted = formatDateTime(value)
  return formatted === '—' ? undefined : formatted
}

function pickLatestPrepTime(...candidates: (string | Date | null | undefined)[]) {
  let latestTs: number | null = null
  let latestRaw: string | Date | undefined
  for (const candidate of candidates) {
    if (candidate == null || candidate === '') continue
    const date = candidate instanceof Date ? candidate : new Date(String(candidate).replace('T', ' '))
    const ts = date.getTime()
    if (Number.isNaN(ts)) continue
    if (latestTs == null || ts > latestTs) {
      latestTs = ts
      latestRaw = candidate
    }
  }
  return latestRaw != null ? formatPrepLastTime(latestRaw) : undefined
}

function touchBuyerExcludeOpTime() {
  lastBuyerExcludeOpAt.value = new Date().toISOString()
}

function statusLabel(s?: string) {
  return statusOptions.value.find((o) => o.value === s)?.label ?? s ?? '—'
}

function syncPendingDecisions(shops: MonthlySettlementShopSummary[]) {
  Object.keys(pendingDecisions).forEach((k) => delete pendingDecisions[Number(k)])
  for (const shop of shops) {
    for (const row of shop.pendingOrders ?? []) {
      if (row.orderId == null) continue
      pendingDecisions[row.orderId] = row.included ?? true
    }
  }
}

function syncSelectedShop() {
  const shops = shopSummaries.value
  if (!shops.length) {
    selectedShopId.value = null
    return
  }
  if (selectedShopId.value == null || !shops.some((s) => s.shopId === selectedShopId.value)) {
    selectedShopId.value = shops[0]?.shopId ?? null
  }
}

function onShopRowClick(row: MonthlySettlementShopSummary) {
  if (row.shopId == null) return
  selectedShopId.value = row.shopId
  maxProfitShowAll.value = false
}

function shopRowClassName({ row }: { row: MonthlySettlementShopSummary }) {
  const classes = []
  if (row.shopId === selectedShopId.value) classes.push('is-selected')
  if (!isShopOrdersImported(row.shopId)) classes.push('is-not-imported')
  return classes.join(' ')
}

function goImportOrders() {
  void router.push({
    path: ecommercePathForModule('order'),
    query: { month: settlementMonth.value },
  })
}

function goMaxProfitOrderDetail() {
  const orderId = displayedMaxProfit.value?.orderId
  if (!orderId) return
  void router.push({
    path: ecommercePathForModule('order'),
    query: { orderId: String(orderId) },
  })
}

function onPrepTaskClick(task: PrepTask) {
  task.action?.()
}

function goReviewPending() {
  const shopWithPending = shopSummaries.value.find((s) => (s.pendingOrderCount ?? 0) > 0)
  if (shopWithPending?.shopId != null) {
    selectedShopId.value = shopWithPending.shopId
    return
  }
  if (selectedShopId.value == null && shopSummaries.value[0]?.shopId != null) {
    selectedShopId.value = shopSummaries.value[0].shopId
  }
}

function beginPrepLoading(silent?: boolean) {
  if (silent) return
  prepLoadingCount += 1
  prepLoading.value = true
}

function endPrepLoading(silent?: boolean) {
  if (silent) return
  prepLoadingCount = Math.max(0, prepLoadingCount - 1)
  if (prepLoadingCount === 0) {
    prepLoading.value = false
  }
}

function beginCalculating() {
  calculatingCount += 1
  calculating.value = true
}

function endCalculating() {
  calculatingCount = Math.max(0, calculatingCount - 1)
  if (calculatingCount === 0) {
    calculating.value = false
  }
}

function beginSubmitting() {
  submittingCount += 1
  submitting.value = true
}

function endSubmitting() {
  submittingCount = Math.max(0, submittingCount - 1)
  if (submittingCount === 0) {
    submitting.value = false
  }
}

function applySettlementResult(data: { shops: MonthlySettlementShopSummary[]; expressBillImported?: boolean; calculatedAt?: string } | null) {
  if (!data) {
    result.value = null
    calculated.value = false
    selectedShopId.value = null
    maxProfitShowAll.value = false
    lastCalculatedAt.value = null
    return
  }
  result.value = data
  calculated.value = true
  syncPendingDecisions(data.shops ?? [])
  syncSelectedShop()
  lastCalculatedAt.value = data.calculatedAt ?? new Date().toISOString()
}

async function runPageLoad(source: 'auto' | 'manual') {
  const month = settlementMonth.value
  if (!month) {
    if (source === 'manual') {
      ElMessage.warning(t('ecommerce.monthlySettlement.monthRequired'))
    }
    return
  }
  if (pageLoadPromise) {
    await pageLoadPromise
    if (source === 'manual') {
      return runPageLoad('manual')
    }
    if (calculated.value && settlementMonth.value === month) {
      return
    }
  }

  pageLoadPromise = (async () => {
    beginCalculating()
    if (source === 'manual') beginSubmitting()
    try {
      await loadPrepData({ silent: true })
      if (settlementMonth.value !== month) return
      if (source === 'manual') {
        const data = await calculateMonthlySettlement(month)
        if (settlementMonth.value !== month) return
        applySettlementResult(data ?? { shops: [] })
        ElMessage.success(t('ecommerce.monthlySettlement.calculateSaved'))
      } else {
        const data = await fetchMonthlySettlementSnapshot(month)
        if (settlementMonth.value !== month) return
        applySettlementResult(data)
      }
    } catch {
      if (settlementMonth.value !== month) return
      if (source === 'auto') {
        applySettlementResult(null)
      }
    } finally {
      endCalculating()
      if (source === 'manual') endSubmitting()
    }
  })().finally(() => {
    pageLoadPromise = null
  })
  return pageLoadPromise
}

async function enter() {
  if (enterPromise) return enterPromise
  enterPromise = (async () => {
    if (!shopOptions.value.length) {
      try {
        shopOptions.value = await fetchShopOptions()
      } catch {
        shopOptions.value = []
      }
    }
    if (!expressStations.value.length) {
      try {
        const page = await fetchExpressStations(undefined, { page: 1, pageSize: 200 })
        expressStations.value = page.records ?? []
      } catch {
        expressStations.value = []
      }
    }
    await runPageLoad('auto')
    bootstrapped = true
  })().finally(() => {
    enterPromise = null
  })
  return enterPromise
}

async function init() {
  await enter()
}

async function onCalculate() {
  if (calculateButtonDisabled.value) return
  await runPageLoad('manual')
}

async function load() {
  await onCalculate()
}

async function loadPrepData(options?: { silent?: boolean }) {
  if (!settlementMonth.value) {
    orderOverview.value = null
    expressBillRecords.value = []
    buyerExcludeCount.value = 0
    buyerExcludesSnapshot.value = []
    expressBillImported.value = false
    prepLoadingCount = 0
    prepLoading.value = false
    return
  }
  const seq = ++prepRequestSeq
  beginPrepLoading(options?.silent)
  try {
    const [overview, imported, records, excludes] = await Promise.all([
      fetchSalesOrderMonthlyOverview(settlementMonth.value),
      fetchExpressBillImported(settlementMonth.value),
      fetchExpressBillRecords(settlementMonth.value),
      fetchSettlementBuyerExcludes(),
    ])
    if (seq !== prepRequestSeq) return
    orderOverview.value = overview
    expressBillImported.value = !!imported
    expressBillRecords.value = records ?? []
    buyerExcludesSnapshot.value = excludes ?? []
    buyerExcludeCount.value = buyerExcludesSnapshot.value.length
  } catch {
    if (seq !== prepRequestSeq) return
    orderOverview.value = null
    expressBillRecords.value = []
    buyerExcludesSnapshot.value = []
    buyerExcludeCount.value = 0
    expressBillImported.value = false
  } finally {
    endPrepLoading(options?.silent)
  }
}

function onPendingDecisionChange(orderId: number, included: boolean) {
  pendingDecisions[orderId] = included
}

function resolvePendingDecision(row: { orderId?: number; included?: boolean | null }) {
  const orderId = row.orderId
  if (orderId == null) return true
  if (pendingDecisions[orderId] === undefined) {
    pendingDecisions[orderId] = row.included ?? true
  }
  return pendingDecisions[orderId]
}

function setAllPendingDecisions(shop: MonthlySettlementShopSummary | null, included: boolean) {
  if (!shop) return
  for (const row of shop.pendingOrders ?? []) {
    if (row.orderId == null) continue
    pendingDecisions[row.orderId] = included
  }
}

async function savePendingDecisions(shop: MonthlySettlementShopSummary) {
  if (!settlementMonth.value) return
  const items = (shop.pendingOrders ?? [])
    .filter((row) => row.orderId != null)
    .map((row) => ({ orderId: row.orderId!, included: pendingDecisions[row.orderId!] ?? true }))
  if (!items.length) {
    ElMessage.warning(t('ecommerce.monthlySettlement.noDecisions'))
    return
  }
  savingDecisions.value = true
  try {
    const data = await saveSettlementOrderDecisions({
      settlementMonth: settlementMonth.value,
      items,
    })
    applySettlementResult(data)
    lastPendingDecisionAt.value = new Date().toISOString()
    ElMessage.success(t('ecommerce.monthlySettlement.decisionsSaved'))
  } finally {
    savingDecisions.value = false
  }
}

async function loadBuyerExcludes() {
  loadingExcludes.value = true
  try {
    buyerExcludes.value = await fetchSettlementBuyerExcludes()
    buyerExcludesSnapshot.value = buyerExcludes.value
    buyerExcludeCount.value = buyerExcludes.value.length
  } finally {
    loadingExcludes.value = false
  }
}

async function addBuyerExclude() {
  const name = excludeFormBuyerName.value.trim()
  if (!name) {
    ElMessage.warning(t('ecommerce.monthlySettlement.buyerNameRequired'))
    return
  }
  savingBuyerExclude.value = true
  try {
    await saveSettlementBuyerExclude({
      shopId: excludeFormShopId.value ?? null,
      buyerName: name,
      remark: excludeFormRemark.value.trim() || undefined,
      enabled: 1,
    })
    excludeFormBuyerName.value = ''
    excludeFormRemark.value = ''
    await loadBuyerExcludes()
    touchBuyerExcludeOpTime()
    ElMessage.success(t('ecommerce.common.saved'))
  } finally {
    savingBuyerExclude.value = false
  }
}

async function removeBuyerExclude(id: number) {
  await deleteSettlementBuyerExclude(id)
  await loadBuyerExcludes()
  touchBuyerExcludeOpTime()
}

function openExpressBillDialog() {
  expressBillVisible.value = true
}

async function onExpressBillImported() {
  if (calculated.value) {
    await runPageLoad('manual')
  } else {
    await loadPrepData()
  }
}

watch(settlementMonth, () => {
  if (ignoreMonthWatch.value) return
  result.value = null
  selectedShopId.value = null
  maxProfitShowAll.value = false
  lastPendingDecisionAt.value = null
  lastCalculatedAt.value = null
  calculated.value = false
  void runPageLoad('auto')
})

watch(buyerExcludeVisible, (visible) => {
  if (visible) void loadBuyerExcludes()
})

onMounted(() => {
  const task = bootstrapped || enterPromise ? (enterPromise ?? Promise.resolve()) : enter()
  void Promise.resolve(task).finally(() => {
    ignoreMonthWatch.value = false
  })
})

defineExpose({ load, enter, init })
</script>

<style scoped lang="scss">
.settlement-center {
  min-height: 100%;

  :deep(.el-table__header .el-table__cell) {
    color: var(--el-text-color-primary);
  }

  :deep(.el-table__header .el-table__cell .cell) {
    color: var(--el-text-color-primary);
  }

  :deep(.el-descriptions__label) {
    color: var(--el-text-color-primary) !important;
  }
}

.settlement-center__layout {
  display: grid;
  grid-template-columns: minmax(220px, 24%) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.settlement-center__head,
.settlement-center__result-head {
  margin-bottom: 12px;
}

.settlement-center__result-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.settlement-center__result-title-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.settlement-center__result-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
  white-space: nowrap;
}

.settlement-center__result-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.settlement-section-title {
  margin: 0;
  padding-left: 10px;
  border-left: 3px solid var(--el-color-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--el-text-color-primary);
}

.settlement-center__subtitle {
  margin: 4px 0 0;
  padding-left: 13px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.settlement-center__left {
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
}

.settlement-center__right {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
}

.settlement-center__controls {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.settlement-center__controls + .settlement-section-title {
  margin-bottom: 10px;
}

.settlement-center__calculate {
  width: 100%;
  height: 44px;
  margin-top: 12px;
  font-size: 16px;
  font-weight: 600;

  &.is-calculate {
    --el-button-bg-color: #15803d;
    --el-button-border-color: #15803d;
    --el-button-hover-bg-color: #166534;
    --el-button-hover-border-color: #166534;
    --el-button-active-bg-color: #14532d;
    --el-button-active-border-color: #14532d;
  }

  &.is-precalculate {
    --el-button-bg-color: #ea580c;
    --el-button-border-color: #ea580c;
    --el-button-hover-bg-color: #c2410c;
    --el-button-hover-border-color: #c2410c;
    --el-button-active-bg-color: #9a3412;
    --el-button-active-border-color: #9a3412;
  }

  &.is-recalculate {
    --el-button-bg-color: #2563eb;
    --el-button-border-color: #2563eb;
    --el-button-hover-bg-color: #1d4ed8;
    --el-button-hover-border-color: #1d4ed8;
    --el-button-active-bg-color: #1e40af;
    --el-button-active-border-color: #1e40af;
  }

  &.is-disabled {
    --el-button-disabled-bg-color: #e5e7eb;
    --el-button-disabled-border-color: #e5e7eb;
    --el-button-disabled-text-color: #9ca3af;
  }

  --el-button-text-color: #fff;
  --el-button-hover-text-color: #fff;
  --el-button-active-text-color: #fff;
}

.settlement-center__calculate-wrap {
  display: block;
  width: 100%;
  margin-top: 12px;

  .settlement-center__calculate {
    margin-top: 0;
  }
}

.settlement-center__calculate-hint {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
  text-align: center;
}

.prep-checklist {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.prep-checklist__item {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: start;
  padding: 12px 12px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--wr-stat-gray-bg, #f3f4f6);

  &.is-clickable {
    cursor: pointer;
    transition: border-color 0.15s ease, background-color 0.15s ease, box-shadow 0.15s ease;

    &:hover {
      box-shadow: var(--wr-shadow, 0 4px 12px rgb(0 0 0 / 5%));

      .prep-checklist__go {
        color: var(--el-text-color-primary);
      }
    }
  }

  &.is-success {
    background: var(--wr-stat-green-bg, #f0fdf4);
    border-color: #bbf7d0;

    &.is-clickable:hover {
      background: #dcfce7;
      border-color: #86efac;
    }
  }

  &.is-warning {
    background: var(--wr-stat-orange-bg, #fff7ed);
    border-color: #fed7aa;

    &.is-clickable:hover {
      background: #ffedd5;
      border-color: #fdba74;
    }
  }

  &.is-danger {
    background: #fef2f2;
    border-color: #fecaca;

    &.is-clickable:hover {
      background: #fee2e2;
      border-color: #fca5a5;
    }
  }

  &.is-muted {
    background: var(--wr-stat-gray-bg, #f3f4f6);
    border-color: #e5e7eb;

    &.is-clickable:hover {
      background: #e5e7eb;
      border-color: #d1d5db;
    }
  }
}

.prep-checklist__dot {
  width: 18px;
  height: 18px;
  margin-top: 2px;
  border-radius: 50%;
  border: 2px solid var(--el-border-color);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #fff;

  &.is-success {
    border-color: var(--el-color-success);
    background: var(--el-color-success);
  }

  &.is-warning {
    border-color: var(--el-color-warning);
    background: var(--el-color-warning);
  }

  &.is-danger {
    border-color: var(--el-color-danger);
    background: var(--el-color-danger);
  }

  &.is-muted {
    background: var(--el-fill-color);
  }
}

.prep-checklist__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.prep-checklist__title-row {
  display: grid;
  grid-template-columns: 6.5em auto;
  align-items: center;
  column-gap: 6px;
}

.prep-checklist__title {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
  min-width: 0;
}

.prep-checklist__status {
  width: fit-content;
  max-width: 100%;
  margin: 0;
  justify-self: start;
  height: 26px;
  padding: 0 10px;
  font-size: 12px;
  line-height: 1;

  :deep(.el-tag__content) {
    line-height: 24px;
  }
}

.prep-checklist__desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.prep-checklist__desc-highlight {
  color: var(--wr-stat-green, #16a34a);
  font-weight: 600;
}

.prep-checklist__subs {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.prep-express-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.prep-express-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
}

.prep-express-card__avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  img {
    width: 24px;
    height: 24px;
    object-fit: contain;
  }

  &.is-custom img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.prep-express-card__body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.prep-express-card__name {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.4;
}

.prep-express-card__match {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}

.prep-express-card__match-count {
  color: var(--wr-stat-green, #16a34a);
  font-weight: 600;
}

.prep-express-card__gap {
  font-size: 11px;
  color: var(--el-color-warning);
  line-height: 1.4;
}

.prep-checklist__sub {
  font-size: 11px;
  color: var(--el-text-color-regular);
  line-height: 1.4;
}

.prep-checklist__time {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  line-height: 1.5;
  font-variant-numeric: tabular-nums;
}

.prep-checklist__go {
  flex-shrink: 0;
  align-self: center;
  font-size: 14px;
  color: var(--el-text-color-placeholder);
}

.settlement-shop-not-imported {
  display: block;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.settlement-shop-table {
  margin-bottom: 16px;

  :deep(.el-table__row) {
    cursor: pointer;
  }

  :deep(.el-table__row.is-not-imported > td.el-table__cell) {
    background: var(--el-fill-color-lighter);
  }

  :deep(.el-table__row.is-not-imported > td.el-table__cell:nth-child(2)) {
    text-align: center !important;
  }

  :deep(.el-table__row.is-selected > td.el-table__cell) {
    background: var(--el-color-primary-light-9) !important;
  }

  :deep(.el-table__row.is-selected > td.el-table__cell:first-child) {
    box-shadow: inset 3px 0 0 #2563eb;
  }
}

.settlement-shop-name {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.settlement-shop-name__avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  flex-shrink: 0;
  object-fit: cover;
  background: #f3f4f6;

  &:not(.is-avatar) {
    object-fit: contain;
    padding: 3px;
  }
}

.settlement-shop-name__text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.settlement-center__right {
  :deep(.el-table__body .el-table__cell) {
    padding-top: 10px;
    padding-bottom: 10px;
  }

  :deep(.el-table__header .el-table__cell) {
    padding-top: 8px;
    padding-bottom: 8px;
  }

  :deep(.el-table__cell) {
    border-right: none !important;
  }

  :deep(.el-table__header .el-table__cell) {
    border-right: none !important;
  }

  :deep(.el-table__inner-wrapper::before),
  :deep(.el-table__border-left-patch) {
    display: none;
  }

  :deep(.el-descriptions__cell) {
    border-right: none !important;
    padding-top: 12px !important;
    padding-bottom: 12px !important;
  }
}

.order-stats-cell {
  font-variant-numeric: tabular-nums;

  .is-included {
    color: var(--el-color-success);
  }

  .is-excluded {
    color: var(--el-text-color-secondary);
  }

  .is-pending {
    color: var(--el-color-warning);
  }
}

.shop-detail {
  margin-top: 4px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.shop-detail__grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.shop-detail-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.shop-detail-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
}

.shop-detail-card__shop-name,
.shop-detail-card__scope {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1;
}

.shop-detail-card__shop-name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 22px;
}

.shop-detail-card__shop-avatar {
  width: 22px;
  height: 22px;
}

.shop-detail-card__all-shops-btn {
  margin-left: auto;
}

.shop-detail-card--max-profit .shop-detail-card__title {
  gap: 6px;
}

.max-profit-card__crown {
  font-size: 14px;
  line-height: 1;
}

.max-profit-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.max-profit-card__overview {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.max-profit-card__thumb {
  flex-shrink: 0;
  width: 72px;
  height: 72px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
}

.max-profit-card__image {
  width: 100%;
  height: 100%;
}

.max-profit-card__thumb-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--el-text-color-placeholder);
  font-size: 24px;
  background: var(--el-fill-color-light);
}

.max-profit-card__facts {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  margin: 0;
}

.max-profit-card__fact {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
  font-size: 12px;
  line-height: 1.45;

  dt {
    margin: 0;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
  }

  dd {
    margin: 0;
    color: var(--el-text-color-primary);
    word-break: break-all;
  }
}

.max-profit-card__fact--order-no,
.max-profit-card__fact--shop {
  grid-template-columns: minmax(0, 1fr);
}

.max-profit-card__fact--order-no dd {
  margin: 0;
}

.max-profit-card__order-link,
.max-profit-card__order-no {
  font-size: 17px;
  font-weight: 700;
  line-height: 1.35;
  word-break: break-all;
}

.max-profit-card__order-link {
  padding: 0;
  border: none;
  background: none;
  color: var(--el-color-primary);
  cursor: pointer;
  text-align: left;

  &:hover {
    text-decoration: underline;
  }
}

.max-profit-card__order-no {
  color: var(--el-text-color-primary);
}

.max-profit-card__shop-line {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.max-profit-card__shop-avatar {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  object-fit: cover;

  &.is-avatar {
    border-radius: 50%;
  }
}

.max-profit-card__rows {
  display: flex;
  flex-direction: column;
  border-top: 1px solid var(--el-border-color-lighter);
}

.max-profit-card__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-size: 13px;
  color: var(--el-text-color-secondary);

  strong {
    color: var(--el-text-color-primary);
    font-weight: 600;
  }

  .is-profit {
    color: var(--el-color-success);
  }
}

.max-profit-card__hero {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 2px;
}

.max-profit-card__hero-label {
  font-size: 13px;
  font-weight: 600;
  color: #991b1b;
}

.max-profit-card__hero-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
  color: #991b1b;

  &.is-unknown {
    font-size: 14px;
    font-weight: 500;
    line-height: 1.45;
    color: var(--el-text-color-secondary);
  }
}

.shop-detail-card__title {
  display: inline-flex;
  align-items: center;
  margin: 0;
  padding-left: 8px;
  border-left: 3px solid var(--el-color-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 22px;
  color: var(--el-text-color-primary);
}

.shop-detail-card__badge {
  display: inline-flex;
  align-items: center;
  align-self: center;
  flex-shrink: 0;
  line-height: 1;

  :deep(.el-badge__content) {
    position: static;
    top: auto;
    transform: none;
  }
}

.shop-detail-card__body {
  padding: 12px;

  &--flush {
    padding: 0;
  }
}

.shop-detail-card--pending .pending-actions {
  padding: 8px 12px 12px;
}

.shop-detail-card__body--flush :deep(.el-empty) {
  padding: 24px 12px;
}

.amount-primary {
  font-weight: 700;
}

.amount-profit {
  color: var(--el-color-success);
  font-weight: 700;
}

.pending-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 0;
}

.pending-actions__include-all {
  --el-button-text-color: #15803d;
  --el-button-border-color: #15803d;
  --el-button-bg-color: #fff;
  --el-button-hover-text-color: #166534;
  --el-button-hover-border-color: #166534;
  --el-button-hover-bg-color: #f0fdf4;
  --el-button-active-text-color: #14532d;
  --el-button-active-border-color: #14532d;
  --el-button-active-bg-color: #dcfce7;
}

.pending-actions__exclude-all {
  --el-button-text-color: #b91c1c;
  --el-button-border-color: #b91c1c;
  --el-button-bg-color: #fff;
  --el-button-hover-text-color: #991b1b;
  --el-button-hover-border-color: #991b1b;
  --el-button-hover-bg-color: #fef2f2;
  --el-button-active-text-color: #7f1d1d;
  --el-button-active-border-color: #7f1d1d;
  --el-button-active-bg-color: #fee2e2;
}

.pending-actions__confirm {
  min-width: 88px;
  height: 36px;
  padding: 0 20px;
  font-size: 14px;
  font-weight: 600;
}

.pending-decision-radio {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;

  :deep(.el-radio.is-include .el-radio__label) {
    color: #15803d;
  }

  :deep(.el-radio.is-include.is-checked .el-radio__inner) {
    border-color: #15803d;
    background: #15803d;
  }

  :deep(.el-radio.is-include.is-checked .el-radio__label) {
    color: #15803d;
    font-weight: 600;
  }

  :deep(.el-radio.is-exclude .el-radio__label) {
    color: #b91c1c;
  }

  :deep(.el-radio.is-exclude.is-checked .el-radio__inner) {
    border-color: #b91c1c;
    background: #b91c1c;
  }

  :deep(.el-radio.is-exclude.is-checked .el-radio__label) {
    color: #b91c1c;
    font-weight: 600;
  }
}

.buyer-exclude-dialog {
  :deep(.el-dialog__body) {
    padding-top: 8px;
  }
}

.buyer-exclude-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.buyer-exclude-panel__head {
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-light);
}

.buyer-exclude-panel__hint {
  margin: 0 0 12px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--el-text-color-secondary);
}

.buyer-exclude-panel__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.buyer-exclude-stat {
  padding: 10px 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);

  &__label {
    display: block;
    font-size: 11px;
    color: var(--el-text-color-secondary);
  }

  &__value {
    display: block;
    margin-top: 4px;
    font-size: 20px;
    line-height: 1.2;
    font-weight: 700;
  }

  &--primary .buyer-exclude-stat__value {
    color: var(--el-color-primary);
  }

  &--warning .buyer-exclude-stat__value {
    color: var(--el-color-warning);
  }
}

.buyer-exclude-form-card {
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: #fff;
}

.buyer-exclude-form-card__title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.buyer-exclude-form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 10px;
}

.buyer-exclude-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 140px;

  &--grow {
    flex: 1;
    min-width: 160px;
  }

  &__label {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__control {
    width: 100%;
  }
}

.buyer-exclude-form__submit {
  flex-shrink: 0;
}

.buyer-exclude-list-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.buyer-exclude-list__toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.buyer-exclude-list__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.buyer-exclude-list__search {
  width: min(100%, 240px);
}

.buyer-exclude-list__empty {
  padding: 24px 12px;
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
}

.buyer-exclude-list__empty-title {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.buyer-exclude-list__empty-desc {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.buyer-exclude-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: min(360px, calc(100vh - 420px));
  overflow-y: auto;
  padding-right: 2px;
}

.buyer-exclude-card {
  display: grid;
  grid-template-columns: minmax(120px, 150px) minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: #fff;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    border-color: var(--el-border-color);
    box-shadow: 0 1px 4px rgba(15, 23, 42, 0.06);
  }
}

.buyer-exclude-card__shop {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.buyer-exclude-card__avatar {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  object-fit: cover;
  flex-shrink: 0;

  &.is-custom {
    border-radius: 50%;
  }
}

.buyer-exclude-card__shop-name {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.buyer-exclude-card__main {
  min-width: 0;
}

.buyer-exclude-card__buyer {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  word-break: break-all;
}

.buyer-exclude-card__remark {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-regular);
  word-break: break-all;
}

.buyer-exclude-card__time {
  margin-top: 4px;
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.buyer-exclude-card__remove {
  flex-shrink: 0;
}

@media (max-width: 1100px) {
  .settlement-center__layout {
    grid-template-columns: 1fr;
  }

  .shop-detail__grid {
    grid-template-columns: 1fr;
  }
}
</style>
