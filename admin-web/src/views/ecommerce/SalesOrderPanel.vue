<template>
  <div class="sales-order-panel">
    <div class="order-workbench-header">
      <div class="order-workbench-header__main">
        <el-date-picker
          v-model="orderMonth"
          type="month"
          value-format="YYYY-MM"
          :placeholder="t('ecommerce.salesOrder.monthPlaceholder')"
          class="order-workbench-header__month"
        />
        <el-button :loading="overviewLoading" @click="refreshAll">{{ t('ecommerce.salesOrder.refresh') }}</el-button>
      </div>
      <p class="order-workbench-hint">{{ t('ecommerce.salesOrder.dataHint') }}</p>
    </div>

    <section v-loading="overviewLoading" class="order-stat-cards">
      <div v-for="card in statCards" :key="card.key" class="order-stat-card" :class="`is-${card.tone}`">
        <div class="order-stat-card__label">{{ card.label }}</div>
        <div class="order-stat-card__value">{{ card.value }}</div>
        <p v-if="card.hint" class="order-stat-card__hint">{{ card.hint }}</p>
      </div>
    </section>

    <section v-loading="overviewLoading" class="order-shop-section">
      <h3 class="order-section-title">{{ t('ecommerce.salesOrder.shopImportSection') }}</h3>
      <div v-if="shopImportCards.length" class="order-shop-grid">
        <div
          v-for="shop in shopImportCards"
          :key="shop.shopId"
          class="order-shop-card"
          :class="[`is-${shop.tone}`, { 'is-active': shopFilter === shop.shopId }]"
          @click="onShopCardClick(shop)"
        >
          <div class="order-shop-card__head">
            <div class="order-shop-card__name-wrap">
              <img
                :src="getShopCardShopIcon(shop).src"
                alt=""
                class="order-shop-card__shop-avatar"
                :class="{ 'is-avatar': getShopCardShopIcon(shop).isCustomAvatar }"
              />
              <span class="order-shop-card__name">{{ shop.shopName }}</span>
            </div>
            <div v-if="shop.platformName" class="order-shop-card__platform-wrap">
              <img
                :src="getShopCardPlatformIcon(shop).src"
                alt=""
                class="order-shop-card__platform-avatar"
                :class="{ 'is-avatar': getShopCardPlatformIcon(shop).isCustomAvatar }"
              />
              <span class="order-shop-card__platform">{{ shop.platformName }}</span>
            </div>
          </div>
          <p class="order-shop-card__status">{{ shop.statusText }}</p>
          <p v-if="shop.dateLabel" class="order-shop-card__date">{{ shop.dateLabel }}</p>
          <div v-if="shop.actionLabel" class="order-shop-card__footer">
            <el-button
              size="small"
              :type="shop.actionType"
              class="order-shop-card__action"
              @click.stop="onShopCardAction(shop)"
            >
              {{ shop.actionLabel }}
            </el-button>
          </div>
        </div>
      </div>
      <el-empty v-else :description="t('ecommerce.salesOrder.noShops')" :image-size="64" />
    </section>

    <section class="order-list-section">
      <h3 class="order-section-title">{{ t('ecommerce.salesOrder.orderListSection') }}</h3>
      <div class="panel-toolbar">
        <span class="panel-toolbar__label">{{ t('ecommerce.salesOrder.orderTimeFilter') }}</span>
        <el-date-picker
          v-model="orderTimeRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          :start-placeholder="t('ecommerce.salesOrder.orderTimeFrom')"
          :end-placeholder="t('ecommerce.salesOrder.orderTimeTo')"
          :clearable="false"
          class="order-time-range"
          @change="onOrderTimeRangeChange"
        />
        <el-input
          v-model="keyword"
          class="panel-search-input"
          :placeholder="t('ecommerce.salesOrder.searchPlaceholder')"
          clearable
          @keyup.enter="searchOrders"
          @clear="searchOrders"
        />
        <el-select v-model="statusFilter" clearable :placeholder="t('ecommerce.salesOrder.statusFilter')" style="width: 140px" @change="() => load(true)">
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-select v-model="shopFilter" clearable filterable :placeholder="t('ecommerce.salesOrder.shop')" style="width: 160px" @change="() => load(true)">
          <el-option v-for="s in shopOptions" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-button type="primary" @click="openImport()">{{ t('ecommerce.salesOrder.import') }}</el-button>
        <el-button @click="openCreate">{{ t('ecommerce.salesOrder.add') }}</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="records"
        stripe
        border
        size="small"
        row-key="id"
        class="sales-order-table"
        :row-class-name="orderRowClassName"
        @row-click="(row: EcSalesOrder) => openDetail(row.id)"
      >
      <el-table-column prop="platformOrderNo" :label="t('ecommerce.salesOrder.platformOrderNo')" width="132" show-overflow-tooltip fixed>
        <template #default="{ row }">
          <span>{{ row.platformOrderNo || '—' }}</span>
          <el-tag v-if="(row.lineCount ?? 0) > 1" size="small" type="info" class="line-count-tag">
            {{ t('ecommerce.salesOrder.lineCountTag', { count: row.lineCount }) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.salesOrder.shop')" width="148" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="order-list-shop-name">
            <img
              :src="getOrderShopIconMeta(row).src"
              alt=""
              class="order-list-shop-name__avatar"
              :class="{ 'is-avatar': getOrderShopIconMeta(row).isCustomAvatar }"
            />
            <span class="order-list-shop-name__text">{{ row.shopName || '—' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.salesOrder.listProductName')" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.linkName || '—' }}</template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.salesOrder.listSkuName')" min-width="110" show-overflow-tooltip>
        <template #default="{ row }">{{ row.skuSpecName || '—' }}</template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.salesOrder.status')" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.salesOrder.orderTime')" width="160">
        <template #default="{ row }">{{ formatDateTime(row.orderTime) }}</template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.salesOrder.receivedAmount')" width="100" align="right">
        <template #default="{ row }"><CnyAmount :value="row.receivedAmount" /></template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.salesOrder.profit')" width="90" align="right">
        <template #default="{ row }"><CnyAmount :value="row.profitAmount" /></template>
      </el-table-column>
      <el-table-column prop="expressStationName" :label="t('ecommerce.salesOrder.expressStation')" width="110" show-overflow-tooltip />
      <el-table-column prop="trackingNumber" :label="t('ecommerce.salesOrder.trackingNumber')" width="120" show-overflow-tooltip />
    </el-table>

    <TablePagination :page="page" :page-size="pageSize" :total="total" @update:page="onPageChange" @update:page-size="onSizeChange" />
    </section>

    <SalesOrderFormDialog
      v-model="dialogVisible"
      :editing-id="editingId"
      :saving="saving"
      :form="form"
      :shop-options="shopOptions"
      :express-options="expressOptions"
      :link-sku-options="linkSkuOptions"
      @save="onSave"
      @shop-change="onShopChange"
      @sync-province="syncProvinceFromAddress"
      @add-line="addLine"
      @remove-line="removeLine"
    />

    <!-- 详情 -->
    <SalesOrderDetailDrawer
      v-model="detailVisible"
      :loading="detailLoading"
      :saving="detailSaving"
      :deleting="deletingDetail"
      :order="detail"
      :shop-icon-meta="detailShopIconMeta"
      :shop-options="shopOptions"
      :express-options="expressOptions"
      :link-sku-options="linkSkuOptions"
      :save-commit-key="detailSaveCommitKey"
      @open="loadDetail"
      @save="onSaveDetail"
      @shop-change="onDetailShopChange"
      @delete="onDeleteDetailOrder"
    />

    <!-- 导入（方案 B：左配置 / 右核对） -->
    <el-dialog
      v-model="importVisible"
      class="import-dialog"
      :title="t('ecommerce.salesOrder.importTitle')"
      width="1200px"
      destroy-on-close
      @closed="resetImport"
      @open="onImportOpen"
    >
      <div v-loading="importResuming" class="import-dialog-body">
        <div class="import-dialog-layout">
          <aside class="import-dialog-config">
            <section class="import-config-section">
              <h4 class="import-section-title">{{ t('ecommerce.salesOrder.importConfigTitle') }}</h4>
              <el-form class="import-form" label-width="72px" label-position="top" hide-required-asterisk>
                <el-form-item required>
                  <template #label>
                    {{ t('ecommerce.salesOrder.shop') }}<span class="import-required-mark">*</span>
                  </template>
                  <el-select
                    v-model="importShopId"
                    filterable
                    style="width: 100%"
                    :disabled="importResumed"
                    @change="onImportShopChange"
                  >
                    <el-option v-for="s in shopOptions" :key="s.id" :label="s.name" :value="s.id" />
                  </el-select>
                </el-form-item>
                <el-form-item v-if="importPlatformName" class="import-platform-item">
                  <template #label>
                    <span class="import-platform-inline">
                      <span>{{ t('ecommerce.salesOrder.platform') }}</span>
                      <el-tag size="small" class="import-platform-tag">{{ importPlatformName }}</el-tag>
                    </span>
                  </template>
                </el-form-item>
              </el-form>
            </section>

            <section class="import-config-section import-config-section--file">
              <h4 class="import-section-title">{{ t('ecommerce.salesOrder.importFileLabel') }}</h4>
              <input
                ref="importReplaceInputRef"
                type="file"
                class="import-file-replace-input"
                accept=".csv,.txt,.xlsx,.xls"
                @change="onImportReplaceFileChange"
              />
              <div
                v-if="showImportFileCard || importFileParsing"
                class="import-file-card"
                :class="{
                  'import-file-card--clickable': !importFileParsing && !reparsing,
                  'import-file-card--parsing': importFileParsing || reparsing,
                }"
                role="button"
                tabindex="0"
                @click="!importFileParsing && !reparsing && triggerImportFileReplace()"
                @keydown.enter.prevent="!importFileParsing && !reparsing && triggerImportFileReplace()"
              >
                <div v-if="importFileParsing || reparsing" class="import-file-card__parsing">
                  <el-icon class="import-file-card__spinner is-loading"><Loading /></el-icon>
                  <p class="import-file-card__parsing-text">
                    {{ reparsing ? t('ecommerce.salesOrder.importFileReparsing') : t('ecommerce.salesOrder.importFileParsing') }}
                  </p>
                  <p v-if="importFileCardName !== '—'" class="import-file-card__parsing-name">{{ importFileCardName }}</p>
                </div>
                <template v-else>
                  <div class="import-file-card__main">
                    <span
                      class="import-file-type-icon"
                      :class="`import-file-type-icon--${importFileCardType.toLowerCase()}`"
                      :aria-label="importFileCardType"
                    />
                    <p class="import-file-card__name">{{ importFileCardName }}</p>
                    <el-icon v-if="importFileCardUploaded" class="import-file-card__check"><CircleCheckFilled /></el-icon>
                  </div>
                  <p v-if="importFileCardMeta" class="import-file-card__meta">{{ importFileCardMeta }}</p>
                </template>
              </div>
              <div v-else-if="!(showImportFileCard || importFileParsing)" class="import-file-block">
                <el-upload
                  ref="importUploadRef"
                  class="import-upload"
                  drag
                  :auto-upload="false"
                  :show-file-list="false"
                  accept=".csv,.txt,.xlsx,.xls"
                  :disabled="!importShopId"
                  @change="onImportUploadChange"
                >
                  <el-icon class="import-upload__icon"><UploadFilled /></el-icon>
                  <p class="import-upload__trigger">{{ t('ecommerce.salesOrder.importUpload') }}</p>
                </el-upload>
                <p class="import-hint import-hint--block">{{ t('ecommerce.salesOrder.importHint') }}</p>
              </div>
              <div v-if="showImportMappingEntry" class="import-mapping-actions">
                <el-button type="primary" link :disabled="!importPlatformId" @click.stop="openMapping">
                  {{ t('ecommerce.salesOrder.configMapping') }}
                </el-button>
                <el-tag v-if="importProfileName" size="small" type="success" effect="plain" class="import-profile-tag">
                  {{ importProfileName }}
                </el-tag>
              </div>
              <div class="import-config-toolbar">
                <div v-if="importPlatformId" class="import-toolbar-group">
                  <ImportStatusMappingEditor
                    :profile-id="importProfileId"
                    :expanded="statusMappingExpanded"
                    @update:expanded="statusMappingExpanded = $event"
                    @saved="onStatusMappingSaved"
                  />
                </div>
                <div v-if="importDetectedColumnCount" class="import-toolbar-group">
                  <button
                    type="button"
                    class="import-toolbar-btn"
                    :class="{ 'import-toolbar-btn--active': detectedColumnsExpanded }"
                    @click="detectedColumnsExpanded = !detectedColumnsExpanded"
                  >
                    <span>{{ t('ecommerce.salesOrder.detectedColumns', { count: importDetectedColumnCount }) }}</span>
                    <el-icon><ArrowDown /></el-icon>
                  </button>
                  <div v-show="detectedColumnsExpanded" class="import-toolbar-panel import-columns-panel">
                    <p class="import-columns-panel__summary">
                      {{ t('ecommerce.salesOrder.detectedColumnsHint', { count: importDetectedColumnCount ?? 0 }) }}
                    </p>
                    <div class="import-columns-tags">
                      <span
                        v-for="(col, index) in importDetectedColumns"
                        :key="`${index}-${col}`"
                        class="import-column-tag"
                        :title="col"
                      >
                        {{ col }}
                      </span>
                    </div>
                  </div>
                </div>
                <div v-if="!importPreview?.batchId" class="import-toolbar-group">
                  <el-button
                    type="primary"
                    class="import-start-parse-btn"
                    :disabled="!canFinishUpload"
                    :loading="uploading"
                    @click="onFinishUpload"
                  >
                    {{ t('ecommerce.salesOrder.startParse') }}
                  </el-button>
                </div>
                <div v-else class="import-toolbar-group">
                  <el-button
                    plain
                    class="import-reparse-btn"
                    :disabled="!canReparseImport"
                    :loading="reparsing"
                    @click="onReparseImport"
                  >
                    {{ t('ecommerce.salesOrder.reparseImport') }}
                  </el-button>
                </div>
              </div>
              <p
                v-if="importPreview?.batchId && importPreview.importFileReadable === false"
                class="import-hint import-hint--warn import-file-missing-hint"
              >
                {{ t('ecommerce.salesOrder.importFileMissingHint') }}
              </p>
            </section>
          </aside>
          <section class="import-dialog-review">
            <div v-if="importReviewBannerVisible" class="import-review-banner">
              <el-icon class="import-review-banner__icon"><InfoFilled /></el-icon>
              <p class="import-review-banner__text">
                {{ t('ecommerce.salesOrder.resumeBatchPrefix') }}
                <strong>{{ importPreview?.batchNo }}</strong>
                {{ t('ecommerce.salesOrder.resumeBatchFileSep') }}
                <strong>{{ importResumedFileName ?? importPreview?.fileName ?? '—' }}</strong>
              </p>
            </div>
            <div class="import-review-head">
              <h4 class="import-panel-title import-panel-title--inline">{{ t('ecommerce.salesOrder.importReviewTitle') }}</h4>
              <div v-if="importPreview" class="import-review-exclude">
                <span class="import-review-exclude__label">{{ t('ecommerce.salesOrder.importExcludeStatuses') }}</span>
                <el-select
                  v-model="importExcludeStatuses"
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  clearable
                  class="import-review-exclude__select"
                  :placeholder="t('ecommerce.salesOrder.importExcludeStatusesPlaceholder')"
                >
                  <el-option
                    v-for="opt in importLineStatusOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </div>
            </div>
            <template v-if="importPreview">
              <div class="import-stats-cards">
                <div class="import-stat-card import-stat-card--matched">
                  <p class="import-stat-card__label">{{ t('ecommerce.salesOrder.importStatMatched') }}</p>
                  <p class="import-stat-card__value">{{ importPreview.matchedRows }}</p>
                </div>
                <div class="import-stat-card import-stat-card--unmatched">
                  <p class="import-stat-card__label">{{ t('ecommerce.salesOrder.importStatUnmatched') }}</p>
                  <p class="import-stat-card__value">{{ importPreview.unmatchedRows }}</p>
                </div>
                <div class="import-stat-card import-stat-card--status">
                  <p class="import-stat-card__label">{{ t('ecommerce.salesOrder.importStatStatusUnmatched') }}</p>
                  <p class="import-stat-card__value">{{ importPreview.statusUnmatchedRows ?? 0 }}</p>
                </div>
                <div class="import-stat-card import-stat-card--error">
                  <p class="import-stat-card__label">{{ t('ecommerce.salesOrder.importStatError') }}</p>
                  <p class="import-stat-card__value">{{ importPreview.errorRows }}</p>
                </div>
              </div>
              <p
                v-if="importPreview.unmatchedRows === 0 && (importPreview.statusUnmatchedRows ?? 0) === 0 && importPreview.batchId"
                class="import-meta import-all-matched"
              >
                {{ t('ecommerce.salesOrder.importAllMatched') }}
              </p>
              <p v-if="importReviewRows.some((row) => row.sellerRemark)" class="import-meta import-hint--block">
                {{ t('ecommerce.salesOrder.sellerRemarkManualHint') }}
              </p>
              <div v-if="importReviewRows.length" class="import-review-table-wrap">
                <div class="import-review-toolbar">
                  <el-input
                    v-model="importReviewSearch"
                    clearable
                    class="import-review-search"
                    :placeholder="t('ecommerce.salesOrder.importReviewSearchPlaceholder')"
                  />
                  <div class="import-review-batch">
                    <div class="import-manual-cost import-review-batch__cost">
                      <span class="import-manual-cost__prefix">￥</span>
                      <el-input
                        v-model="importReviewBatchCost"
                        class="import-manual-cost__input"
                        inputmode="decimal"
                        :placeholder="t('ecommerce.salesOrder.importReviewBatchCostPlaceholder')"
                        @keyup.enter="applyImportReviewBatchCost"
                        @update:model-value="(v: string) => { importReviewBatchCost = sanitizeManualCostInput(v) }"
                      />
                    </div>
                    <el-button
                      type="primary"
                      class="import-review-batch__btn"
                      :disabled="!importReviewDisplayRows.length"
                      @click="applyImportReviewBatchCost"
                    >
                      {{ t('ecommerce.salesOrder.importReviewBatchFill') }}
                    </el-button>
                  </div>
                  <p v-if="importReviewSearch.trim()" class="import-review-toolbar__hint">
                    {{
                      t('ecommerce.salesOrder.importReviewSearchResult', {
                        count: importReviewDisplayRows.length,
                        unmatched: importReviewDisplayUnmatchedCount,
                      })
                    }}
                  </p>
                </div>
                <el-table
                  v-if="importReviewDisplayRows.length"
                  :data="importReviewDisplayRows"
                  class="import-review-table"
                  border
                  size="small"
                  height="100%"
                >
                  <el-table-column prop="rowNo" :label="'#'" width="50" fixed align="center" header-align="center" />
                  <el-table-column prop="platformOrderNo" :label="t('ecommerce.salesOrder.platformOrderNo')" min-width="120" show-overflow-tooltip header-align="center" />
                  <el-table-column prop="linkName" :label="t('ecommerce.salesOrder.linkName')" min-width="160" show-overflow-tooltip header-align="center" />
                  <el-table-column prop="skuSpecName" :label="t('ecommerce.salesOrder.skuSpecName')" min-width="120" show-overflow-tooltip header-align="center" />
                  <el-table-column prop="skuQuantity" :label="t('ecommerce.salesOrder.skuQuantity')" width="72" align="center" header-align="center">
                    <template #default="{ row }">
                      {{ row.skuQuantity ?? '—' }}
                    </template>
                  </el-table-column>
                  <el-table-column :label="t('ecommerce.salesOrder.lineStatus')" width="130" align="center" header-align="center">
                    <template #default="{ row }">
                      <div class="import-line-status-cell">
                        <el-select
                          v-if="row.statusMatchStatus === 'UNMATCHED'"
                          v-model="row.lineStatus"
                          size="small"
                          :placeholder="t('ecommerce.salesOrder.importSelectLineStatus')"
                          class="import-line-status-select"
                        >
                          <el-option
                            v-for="opt in importLineStatusOptions"
                            :key="opt.value"
                            :label="opt.label"
                            :value="opt.value"
                          />
                        </el-select>
                        <el-tag
                          v-else
                          size="small"
                          :type="importLineStatusTagType(row.lineStatus)"
                          effect="light"
                          class="import-line-status-tag"
                          :title="row.platformLineStatus ? t('ecommerce.salesOrder.importPlatformStatusTip', { text: row.platformLineStatus }) : undefined"
                        >
                          {{ importLineStatusLabel(row.lineStatus) }}
                        </el-tag>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column :label="t('ecommerce.salesOrder.manualCost')" width="140" header-align="center">
                    <template #default="{ row }">
                      <div v-if="row.matchStatus === 'UNMATCHED'" class="import-manual-cost">
                        <span class="import-manual-cost__prefix">￥</span>
                        <el-input
                          :model-value="manualCostInputValue(row)"
                          size="small"
                          class="import-manual-cost__input"
                          inputmode="decimal"
                          @update:model-value="(v: string) => onManualCostInput(row, v)"
                          @blur="onManualCostBlur(row)"
                        />
                      </div>
                      <span v-else class="import-meta">—</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="sellerRemark" :label="t('ecommerce.salesOrder.sellerRemark')" min-width="100" show-overflow-tooltip header-align="center" />
                  <el-table-column prop="errorMessage" :label="t('ecommerce.salesOrder.error')" min-width="120" show-overflow-tooltip header-align="center" />
                </el-table>
                <el-empty
                  v-else
                  :description="t('ecommerce.salesOrder.importReviewSearchEmpty')"
                  :image-size="64"
                  class="import-review-search-empty"
                />
              </div>
              <el-empty
                v-else
                :description="t('ecommerce.salesOrder.importReviewNoRows')"
                :image-size="72"
                class="import-review-empty"
              />
            </template>
            <el-empty
              v-else
              :description="t('ecommerce.salesOrder.importReviewEmpty')"
              :image-size="88"
              class="import-review-empty import-review-empty--placeholder"
            />
          </section>
        </div>
      </div>
      <template #footer>
        <div class="import-dialog-footer">
          <p v-if="importPendingCostCount > 0" class="import-footer-hint">
            {{ t('ecommerce.salesOrder.importPendingCostFooter', { count: importPendingCostCount }) }}
          </p>
          <div class="import-dialog-footer__actions">
            <el-button @click="importVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
            <el-button type="success" :disabled="!canCommitImport" :loading="importing" @click="onCommitImport">
              {{ t('ecommerce.salesOrder.commitImport') }}
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <ImportMappingDialog
      v-model="mappingVisible"
      :platform-id="importPlatformId"
      :platform-name="importPlatformName"
      :platform-code="importShop?.platformCode"
      :shop-id="importShopId"
      :doc-columns="importDetectedColumns"
      :file-type="parsedSpreadsheet?.fileType"
      :header-row="parsedSpreadsheet?.headerRow"
      :data-start-row="parsedSpreadsheet?.dataStartRow"
      :initial-profile-id="importProfileId"
      @saved="onMappingSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type UploadFile, type UploadInstance } from 'element-plus'
import { ArrowDown, CircleCheckFilled, InfoFilled, Loading, UploadFilled } from '@element-plus/icons-vue'
import { fetchShopOptions, type EcShop } from '@/api/ecommerce/shop'
import { fetchExpressStations, type EcExpressStation } from '@/api/ecommerce/express'
import { fetchListingLink, fetchListingLinks } from '@/api/ecommerce/listingLink'
import {
  commitSalesOrderImport,
  createSalesOrder,
  deleteSalesOrder,
  fetchSalesOrder,
  fetchSalesOrderImportPreview,
  fetchSalesOrderMonthlyOverview,
  fetchSalesOrders,
  reparseSalesOrderImport,
  replaceSalesOrderImportFile,
  uploadSalesOrderImport,
  updateSalesOrder,
  type EcSalesOrder,
  type EcSalesOrderImportPreview,
  type EcSalesOrderImportRow,
  type EcSalesOrderSaveRequest,
  type ImportRowPatchItem,
  type EcSalesOrderLineSaveItem,
  type EcSalesOrderMonthlyOverview,
  type EcSalesOrderShopImportStatus,
  type ShopImportStatus,
} from '@/api/ecommerce/salesOrder'
import SalesOrderFormDialog from './SalesOrderFormDialog.vue'
import SalesOrderDetailDrawer from './SalesOrderDetailDrawer.vue'
import TablePagination from '@/components/TablePagination.vue'
import CnyAmount from '@/components/CnyAmount.vue'
import ImportMappingDialog from '@/components/ImportMappingDialog.vue'
import ImportStatusMappingEditor from '@/components/ImportStatusMappingEditor.vue'
import { detectSpreadsheetColumns, type ParsedSpreadsheet } from '@/utils/spreadsheetParse'
import { useEcSettingsStore } from '@/stores/ecSettings'
import { BIZ_SALES_ORDER, createImportProfile, defaultPlatformProfileName, fetchImportFields, fetchImportProfiles, type SysImportProfile } from '@/api/sys/import'
import { filterImportFields } from '@/constants/importFieldKeys'
import { buildColumnMappingForUpload } from '@/utils/importColumnMapping'
import { resolveShopIconMeta } from '@/utils/shopVisual'
import { resolvePlatformIconMeta } from '@/utils/platformVisual'
import { usePagination } from '@/composables/usePagination'
import { defaultOrderMonth, formatDateTime, formatMonthDay, monthDateRange, todayDateString } from '@/utils/date'
import { normalizeLineStatus, type ImportLineStatus } from '@/constants/importStatusMapping'
import { parseProvinceFromAddress } from '@/utils/addressProvince'

const { t } = useI18n()
const ecSettings = useEcSettingsStore()
const route = useRoute()
const router = useRouter()

function parseOrderMonthFromQuery(raw: unknown): string | null {
  const value = typeof raw === 'string' ? raw.trim() : Array.isArray(raw) ? raw[0]?.trim() : ''
  if (!value || !/^\d{4}-\d{2}$/.test(value)) return null
  return value
}

const initialRouteMonth = parseOrderMonthFromQuery(route.query.month)

const orderMonth = ref(initialRouteMonth ?? defaultOrderMonth())
const orderTimeRange = ref<[string, string] | null>(monthDateRange(orderMonth.value))
let overviewSeq = 0
const overview = ref<EcSalesOrderMonthlyOverview | null>(null)
const overviewLoading = ref(false)

const keyword = ref('')
const statusFilter = ref<string | undefined>()
const shopFilter = ref<number | undefined>()
const shopOptions = ref<EcShop[]>([])
const expressOptions = ref<EcExpressStation[]>([])

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailSaving = ref(false)
const detailSaveCommitKey = ref(0)
const deletingDetail = ref(false)
const detailId = ref<number | null>(null)
const detail = ref<EcSalesOrder | null>(null)

const importVisible = ref(false)
const importShopId = ref<number | undefined>()
const importPreview = ref<EcSalesOrderImportPreview | null>(null)
const importing = ref(false)
const uploading = ref(false)
const mappingVisible = ref(false)
const importFile = ref<File | null>(null)
const parsedSpreadsheet = ref<ParsedSpreadsheet | null>(null)
const importProfileId = ref<number | null>(null)
const importProfileName = ref<string | null>(null)
const importUploadRef = ref<UploadInstance>()
const importReplaceInputRef = ref<HTMLInputElement>()
const statusMappingExpanded = ref(false)
const detectedColumnsExpanded = ref(false)
const reparsing = ref(false)
const importFileParsing = ref(false)
const manualCostDrafts = ref<Record<number, string>>({})
const pendingResumeBatchId = ref<number | null>(null)
const importResumed = ref(false)
const importResumedFileName = ref<string | null>(null)
const importResuming = ref(false)

const importShop = computed(() => shopOptions.value.find((s) => s.id === importShopId.value))

const shopOptionMap = computed(() => {
  const map = new Map<number, EcShop>()
  for (const shop of shopOptions.value) {
    map.set(shop.id, shop)
  }
  return map
})

function getOrderShopIconMeta(order?: EcSalesOrder | null) {
  if (!order) {
    return resolveShopIconMeta()
  }
  const shop = shopOptionMap.value.get(order.shopId)
  return resolveShopIconMeta(
    order.shopName ?? shop?.name,
    shop?.platformName ?? order.platformName,
    shop?.platformCode,
    shop?.avatarUrl,
  )
}

function getShopCardShopIcon(shop: ShopImportCardView) {
  const opt = shopOptionMap.value.get(shop.shopId)
  return resolveShopIconMeta(
    shop.shopName,
    shop.platformName ?? opt?.platformName,
    shop.platformCode ?? opt?.platformCode,
    shop.shopAvatarUrl ?? opt?.avatarUrl,
  )
}

function getShopCardPlatformIcon(shop: ShopImportCardView) {
  const opt = shopOptionMap.value.get(shop.shopId)
  return resolvePlatformIconMeta(
    shop.platformName ?? opt?.platformName,
    shop.platformCode ?? opt?.platformCode,
    shop.platformAvatarUrl,
  )
}

const detailShopIconMeta = computed(() => getOrderShopIconMeta(detail.value))
const importPlatformId = computed(() => importShop.value?.platformId)
const importPlatformName = computed(() => importShop.value?.platformName ?? '')

const canFinishUpload = computed(
  () => !!importFile.value && !!importShopId.value && !!parsedSpreadsheet.value && !uploading.value,
)

const importExcludeStatuses = ref<ImportLineStatus[]>([])
const importReviewSearch = ref('')
const importReviewBatchCost = ref('')

function isImportRowExcludedByStatus(row: EcSalesOrderImportRow): boolean {
  if (!row.lineStatus) return false
  return importExcludeStatuses.value.includes(normalizeLineStatus(row.lineStatus))
}

const canCommitImport = computed(() => {
  const preview = importPreview.value
  if (!preview?.batchId) return false
  return preview.rows.some((row) => isImportRowImportable(row))
})

function isImportRowImportable(row: EcSalesOrderImportPreview['rows'][number]) {
  if (row.parseStatus !== 'OK') return false
  if (row.statusMatchStatus === 'UNMATCHED' && !row.lineStatus) return false
  if (isImportRowExcludedByStatus(row) && row.matchStatus === 'UNMATCHED') return false
  if (row.matchStatus === 'UNMATCHED') {
    const cost = resolveRowManualCost(row)
    return cost != null && cost !== 0
  }
  return true
}

const importReviewRows = computed(() => {
  const preview = importPreview.value
  if (!preview?.rows) return []
  return preview.rows.filter(
    (row) =>
      row.parseStatus === 'OK'
      && (row.matchStatus === 'UNMATCHED' || row.statusMatchStatus === 'UNMATCHED')
      && !isImportRowExcludedByStatus(row),
  )
})

const importReviewDisplayRows = computed(() => {
  const query = importReviewSearch.value.trim().toLowerCase()
  if (!query) return importReviewRows.value
  return importReviewRows.value.filter((row) => {
    const linkName = (row.linkName ?? '').toLowerCase()
    const skuSpecName = (row.skuSpecName ?? '').toLowerCase()
    return linkName.includes(query) || skuSpecName.includes(query)
  })
})

const importReviewDisplayUnmatchedCount = computed(() =>
  importReviewDisplayRows.value.filter((row) => row.matchStatus === 'UNMATCHED').length,
)

const importPendingCostCount = computed(() =>
  importReviewRows.value.filter((row) => {
    if (row.matchStatus !== 'UNMATCHED') return false
    const cost = resolveRowManualCost(row)
    return cost == null || cost === 0
  }).length,
)

type ImportFileCardType = 'CSV' | 'XLSX' | 'XLS'

const showImportFileCard = computed(
  () => !!(importPreview.value?.batchId || importFile.value || importResumed.value),
)

const showImportMappingEntry = computed(
  () => showImportFileCard.value && !importFileParsing.value && !reparsing.value,
)

const importReviewBannerVisible = computed(
  () => importResumed.value && !!importPreview.value?.batchNo,
)

const importFileCardName = computed(
  () => importResumedFileName.value ?? importFile.value?.name ?? importPreview.value?.fileName ?? '—',
)

const importFileCardUploaded = computed(() => !!importPreview.value?.batchId)

const canReparseImport = computed(() => {
  const preview = importPreview.value
  if (!preview?.batchId) return false
  return preview.importFileReadable !== false
})

const importFileCardType = computed((): ImportFileCardType => {
  if (parsedSpreadsheet.value?.fileType) return parsedSpreadsheet.value.fileType
  const name = importFileCardName.value.toLowerCase()
  if (name.endsWith('.csv') || name.endsWith('.txt')) return 'CSV'
  if (name.endsWith('.xls') && !name.endsWith('.xlsx')) return 'XLS'
  return 'XLSX'
})

const importFileCardMeta = computed(() => {
  const parts: string[] = []
  const totalRows = importPreview.value?.totalRows
  const colCount = importDetectedColumnCount.value
  if (totalRows != null) {
    parts.push(t('ecommerce.salesOrder.importFileMetaRows', { count: totalRows }))
  } else if (colCount != null) {
    parts.push(t('ecommerce.salesOrder.importFileMetaCols', { count: colCount }))
  }
  const size = importPreview.value?.fileSize ?? importFile.value?.size
  if (size != null && size > 0) {
    parts.push(formatImportFileSize(size))
  }
  const batch = importPreview.value?.batchNo
  if (batch) {
    parts.push(batch)
  }
  return parts.join(' · ')
})

const importDetectedColumnCount = computed(() => {
  if (parsedSpreadsheet.value?.columns.length) {
    return parsedSpreadsheet.value.columns.length
  }
  return importPreview.value?.detectedColumnCount ?? null
})

const importDetectedColumns = computed(() => {
  if (parsedSpreadsheet.value?.columns.length) {
    return parsedSpreadsheet.value.columns
  }
  return importPreview.value?.detectedColumns ?? []
})

function formatImportFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) {
    const kb = bytes / 1024
    return kb < 10 ? `${kb.toFixed(1)} KB` : `${Math.round(kb)} KB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function sanitizeManualCostInput(raw: string): string {
  if (!raw) return ''
  let value = raw.trim()
  let sign = ''
  if (value.startsWith('-')) {
    sign = '-'
    value = value.slice(1)
  }
  value = value.replace(/[^\d.]/g, '')
  const dotIndex = value.indexOf('.')
  if (dotIndex >= 0) {
    value = value.slice(0, dotIndex + 1) + value.slice(dotIndex + 1).replace(/\./g, '')
  }
  const [intPart = '', fracPart = ''] = value.split('.')
  const normalized = fracPart.length > 0
    ? `${intPart}.${fracPart.slice(0, 2)}`
    : intPart
  if (sign && !normalized && raw.includes('-')) return '-'
  return `${sign}${normalized}`
}

function parseManualCostNumber(raw: string): number | undefined {
  if (!raw || raw === '-' || raw === '.' || raw === '-.') return undefined
  const num = Number(raw)
  return Number.isFinite(num) ? num : undefined
}

function resolveRowManualCost(row: EcSalesOrderImportPreview['rows'][number]): number | undefined {
  if (row.id != null && manualCostDrafts.value[row.id] !== undefined) {
    const fromDraft = parseManualCostNumber(manualCostDrafts.value[row.id])
    if (fromDraft != null) return fromDraft
  }
  return row.manualCostPrice ?? undefined
}

function manualCostInputValue(row: EcSalesOrderImportPreview['rows'][number]) {
  if (row.id != null && manualCostDrafts.value[row.id] !== undefined) {
    return manualCostDrafts.value[row.id]
  }
  if (row.manualCostPrice == null) return ''
  return String(row.manualCostPrice)
}

function onManualCostInput(row: EcSalesOrderImportPreview['rows'][number], raw: string) {
  const sanitized = sanitizeManualCostInput(raw)
  if (row.id != null) {
    manualCostDrafts.value[row.id] = sanitized
  }
  row.manualCostPrice = parseManualCostNumber(sanitized) ?? undefined
}

function onManualCostBlur(row: EcSalesOrderImportPreview['rows'][number]) {
  if (row.id == null) return
  const parsed = row.manualCostPrice
  if (parsed != null && Number.isFinite(parsed)) {
    const rounded = Math.round(parsed * 100) / 100
    row.manualCostPrice = rounded
    manualCostDrafts.value[row.id] = String(rounded)
    return
  }
  delete manualCostDrafts.value[row.id]
  row.manualCostPrice = undefined
}

function applyImportReviewBatchCost() {
  const sanitized = sanitizeManualCostInput(importReviewBatchCost.value)
  importReviewBatchCost.value = sanitized
  const cost = parseManualCostNumber(sanitized)
  if (cost == null || cost === 0) {
    ElMessage.warning(t('ecommerce.salesOrder.importReviewBatchCostInvalid'))
    return
  }
  const rounded = Math.round(cost * 100) / 100
  let filled = 0
  for (const row of importReviewRows.value) {
    if (row.matchStatus !== 'UNMATCHED' || row.id == null) continue
    row.manualCostPrice = rounded
    manualCostDrafts.value[row.id] = String(rounded)
    filled += 1
  }
  if (!filled) {
    ElMessage.info(t('ecommerce.salesOrder.importReviewBatchFillNone'))
    return
  }
  ElMessage.success(t('ecommerce.salesOrder.importReviewBatchFillSuccess', { count: filled }))
}

const importLineStatusOptions = computed(() => {
  const values: ImportLineStatus[] = [
    'PAID', 'SHIPPED', 'COMPLETED', 'CANCELLED', 'PARTIAL_REFUND', 'REFUNDED', 'RETURNED',
  ]
  return values.map((value) => ({ value, label: importLineStatusLabel(value) }))
})

function importLineStatusLabel(status?: string | null) {
  if (!status) return '—'
  const key = normalizeLineStatus(status)
  const map: Record<ImportLineStatus, string> = {
    PAID: t('ecommerce.salesOrder.importLineStatusPaid'),
    SHIPPED: t('ecommerce.salesOrder.importLineStatusShipped'),
    COMPLETED: t('ecommerce.salesOrder.importLineStatusCompleted'),
    CANCELLED: t('ecommerce.salesOrder.importLineStatusCancelled'),
    PARTIAL_REFUND: t('ecommerce.salesOrder.importLineStatusPartialRefund'),
    REFUNDED: t('ecommerce.salesOrder.importLineStatusRefunded'),
    RETURNED: t('ecommerce.salesOrder.importLineStatusReturned'),
  }
  return map[key]
}

function importLineStatusTagType(status?: string | null) {
  if (!status) return 'info'
  const key = normalizeLineStatus(status)
  const map: Record<ImportLineStatus, 'success' | 'warning' | 'info' | 'danger' | 'primary'> = {
    PAID: 'warning',
    SHIPPED: 'primary',
    COMPLETED: 'success',
    CANCELLED: 'info',
    PARTIAL_REFUND: 'danger',
    REFUNDED: 'danger',
    RETURNED: 'info',
  }
  return map[key]
}

type LineFormRow = EcSalesOrderLineSaveItem & { _pickerKey?: string }

const form = reactive<{
  shopId: number | undefined
  expressStationId: number | undefined
  orderTime: string
  payTime: string
  platformStatus: string
  platformOrderNo: string
  receivedAmount: number | undefined
  trackingNumber: string
  receiveAddress: string
  receiveProvince: string
  buyerRemark: string
  sellerRemark: string
  lines: LineFormRow[]
}>({
  shopId: undefined,
  expressStationId: undefined,
  orderTime: '',
  payTime: '',
  platformStatus: '',
  platformOrderNo: '',
  receivedAmount: undefined,
  trackingNumber: '',
  receiveAddress: '',
  receiveProvince: '',
  buyerRemark: '',
  sellerRemark: '',
  lines: [],
})

const linkSkuOptions = ref<{ key: string; label: string; linkName: string; skuSpecName: string; listingLinkSkuId: number }[]>([])

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

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } = usePagination(
  (p, ps) => {
    const { orderTimeFrom, orderTimeTo } = resolveOrderTimeQuery()
    return fetchSalesOrders(
      keyword.value.trim() || undefined,
      statusFilter.value,
      shopFilter.value,
      orderTimeFrom,
      orderTimeTo,
      { page: p, pageSize: ps },
    )
  },
)

function resolveOrderTimeQuery(): { orderTimeFrom: string; orderTimeTo: string } {
  const range = orderTimeRange.value?.length === 2
    ? orderTimeRange.value
    : monthDateRange(orderMonth.value)
  return { orderTimeFrom: range[0], orderTimeTo: range[1] }
}

function onOrderTimeRangeChange(val: [string, string] | null) {
  if (!val || val.length !== 2) {
    syncTimeRangeFromMonth()
  }
  void load(true)
}

function syncTimeRangeFromMonth() {
  if (!orderMonth.value) return
  orderTimeRange.value = monthDateRange(orderMonth.value)
}

interface ShopImportCardView {
  shopId: number
  shopName: string
  platformName?: string
  platformCode?: number | null
  shopAvatarUrl?: string | null
  platformAvatarUrl?: string | null
  status: ShopImportStatus
  orderCount: number
  statusText: string
  dateLabel?: string
  tone: 'green' | 'gray' | 'orange'
  actionLabel?: string
  actionType?: 'primary' | 'warning' | 'default'
  pendingBatchId?: number | null
}

const statCards = computed(() => {
  const data = overview.value
  return [
    {
      key: 'orders',
      label: t('ecommerce.salesOrder.statImportedOrders'),
      value: `${data?.totalOrderCount ?? 0}`,
      hint: t('ecommerce.salesOrder.statImportedOrdersUnit'),
      tone: 'blue',
    },
    {
      key: 'shops',
      label: t('ecommerce.salesOrder.statShopsDone'),
      value: `${data?.importedShopCount ?? 0}/${data?.totalShopCount ?? 0}`,
      hint: undefined,
      tone: 'green',
    },
    {
      key: 'pending',
      label: t('ecommerce.salesOrder.statPendingReview'),
      value: `${data?.pendingReviewCount ?? 0}`,
      hint: t('ecommerce.salesOrder.statPendingReviewUnit'),
      tone: 'orange',
    },
    {
      key: 'lastImport',
      label: t('ecommerce.salesOrder.statLastImport'),
      value: data?.lastImportTime ? formatMonthDay(data.lastImportTime) : '—',
      hint: data?.lastImportTime ? undefined : t('ecommerce.salesOrder.statLastImportEmpty'),
      tone: 'gray',
    },
  ]
})

const shopImportCards = computed<ShopImportCardView[]>(() => {
  const shops = overview.value?.shops ?? []
  return shops.map((shop) => toShopImportCard(shop))
})

function toShopImportCard(shop: EcSalesOrderShopImportStatus): ShopImportCardView {
  const base = {
    shopId: shop.shopId,
    shopName: shop.shopName || `#${shop.shopId}`,
    platformName: shop.platformName,
    platformCode: shop.platformCode,
    shopAvatarUrl: shop.shopAvatarUrl,
    platformAvatarUrl: shop.platformAvatarUrl,
    status: shop.status,
    orderCount: shop.orderCount,
    pendingBatchId: shop.pendingBatchId,
  }
  if (shop.status === 'PENDING_REVIEW') {
    return {
      ...base,
      tone: 'orange',
      statusText: shop.orderCount > 0
        ? t('ecommerce.salesOrder.shopStatusImportedWithPending', {
            imported: shop.orderCount,
            pending: shop.pendingReviewRows ?? 0,
          })
        : t('ecommerce.salesOrder.shopStatusPendingReview', { count: shop.pendingReviewRows ?? 0 }),
      dateLabel: shop.lastImportTime ? formatMonthDay(shop.lastImportTime) : undefined,
      actionLabel: t('ecommerce.salesOrder.shopContinueReview'),
      actionType: 'warning',
    }
  }
  if (shop.status === 'IMPORTED') {
    return {
      ...base,
      tone: 'green',
      statusText: t('ecommerce.salesOrder.shopStatusImported', { count: shop.orderCount }),
      dateLabel: shop.lastImportTime ? formatMonthDay(shop.lastImportTime) : undefined,
      actionLabel: t('ecommerce.salesOrder.shopViewOrders'),
      actionType: 'default',
    }
  }
  return {
    ...base,
    tone: 'gray',
    statusText: t('ecommerce.salesOrder.shopStatusNotImported'),
    actionLabel: t('ecommerce.salesOrder.shopGoImport'),
    actionType: 'primary',
  }
}

async function loadOverview() {
  const month = orderMonth.value
  if (!month) return
  const seq = ++overviewSeq
  overviewLoading.value = true
  try {
    const data = await fetchSalesOrderMonthlyOverview(month)
    if (seq !== overviewSeq || month !== orderMonth.value) return
    overview.value = data
  } finally {
    if (seq === overviewSeq) overviewLoading.value = false
  }
}

async function refreshAll() {
  syncTimeRangeFromMonth()
  await loadOverview()
  await load(true)
}

function onShopCardClick(shop: ShopImportCardView) {
  shopFilter.value = shop.shopId
  void load(true)
}

function onShopCardAction(shop: ShopImportCardView) {
  if (shop.status === 'IMPORTED') {
    shopFilter.value = shop.shopId
    void load(true)
    return
  }
  openImport(shop.shopId, shop.pendingBatchId ?? undefined)
}

function openImport(shopId?: number, batchId?: number) {
  resetImport()
  importShopId.value = shopId ?? shopFilter.value ?? shopOptions.value[0]?.id
  pendingResumeBatchId.value = batchId ?? null
  importVisible.value = true
}

async function resumePendingBatch(batchId: number) {
  importResuming.value = true
  try {
    const preview = await fetchSalesOrderImportPreview(batchId)
    importPreview.value = preview
    if (preview.shopId) {
      importShopId.value = preview.shopId
    }
    if (preview.profileId) {
      importProfileId.value = preview.profileId
      importProfileName.value = null
    } else {
      await loadImportProfileForPlatform()
    }
    importResumed.value = true
    importResumedFileName.value = preview.fileName ?? preview.batchNo
  } catch {
    ElMessage.error(t('ecommerce.salesOrder.resumeBatchFailed'))
  } finally {
    importResuming.value = false
  }
}

function searchOrders() {
  load(true)
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => load(true), 300)
})

function statusLabel(s?: string) {
  return statusOptions.value.find((o) => o.value === s)?.label ?? s ?? '—'
}

function statusTagType(s?: string) {
  if (s === 'DRAFT') return 'info'
  if (s === 'PAID') return 'primary'
  if (s === 'PARTIAL_SHIPPED') return 'warning'
  if (s === 'SHIPPED') return 'warning'
  if (s === 'PARTIAL_REFUND') return 'danger'
  if (s === 'REFUNDED' || s === 'CANCELLED') return 'danger'
  if (s === 'COMPLETED') return 'success'
  return undefined
}

function emptyLine(): LineFormRow {
  return { skuQuantity: 1, linkName: '', skuSpecName: '', lineReceivedAmount: undefined }
}

function syncProvinceFromAddress() {
  form.receiveProvince = parseProvinceFromAddress(form.receiveAddress) ?? ''
}

function resetForm() {
  form.shopId = shopOptions.value[0]?.id
  form.expressStationId = undefined
  form.orderTime = `${todayDateString()} 00:00:00`
  form.payTime = form.orderTime
  form.platformStatus = '已完成'
  form.platformOrderNo = ''
  form.receivedAmount = undefined
  form.trackingNumber = ''
  form.receiveAddress = ''
  form.receiveProvince = ''
  form.buyerRemark = ''
  form.sellerRemark = ''
  form.lines = [emptyLine()]
  if (form.shopId) loadLinkSkuOptions(form.shopId)
}

async function loadLinkSkuOptions(shopId: number) {
  linkSkuOptions.value = []
  const pageResult = await fetchListingLinks(undefined, shopId, undefined, { page: 1, pageSize: 100 })
  const opts: typeof linkSkuOptions.value = []
  for (const link of pageResult.records ?? []) {
    const d = await fetchListingLink(link.id)
    for (const sku of d.skus ?? []) {
      if (!sku.id) continue
      const key = `${d.name}|||${sku.skuName}`
      opts.push({
        key,
        label: `${d.name} · ${sku.skuName}`,
        linkName: d.name,
        skuSpecName: sku.skuName ?? '',
        listingLinkSkuId: sku.id,
      })
    }
  }
  linkSkuOptions.value = opts
}

function onShopChange(shopId: number) {
  if (shopId) void loadLinkSkuOptions(shopId)
}

watch(dialogVisible, (visible) => {
  if (visible && form.shopId) {
    void loadLinkSkuOptions(form.shopId)
  }
})

function addLine() {
  form.lines.push(emptyLine())
}

function removeLine(index: number) {
  form.lines.splice(index, 1)
}

function onDetailShopChange(shopId: number) {
  if (shopId) void loadLinkSkuOptions(shopId)
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openDetail(id: number) {
  detailId.value = id
  detailVisible.value = true
}

function orderRowClassName({ row }: { row: EcSalesOrder }) {
  return row.id === detailId.value && detailVisible.value ? 'is-selected' : ''
}

function openDetailFromRouteQuery() {
  const raw = route.query.orderId
  const id = typeof raw === 'string' ? Number(raw) : Array.isArray(raw) ? Number(raw[0]) : NaN
  if (!Number.isFinite(id) || id <= 0) return
  openDetail(id)
  const nextQuery = { ...route.query }
  delete nextQuery.orderId
  void router.replace({ path: route.path, query: nextQuery })
}

function applyMonthFromRouteQuery() {
  const month = parseOrderMonthFromQuery(route.query.month)
  if (!month || month === orderMonth.value) return false
  orderMonth.value = month
  orderTimeRange.value = monthDateRange(month)
  return true
}

async function loadDetail() {
  if (!detailId.value) return
  detailLoading.value = true
  try {
    detail.value = await fetchSalesOrder(detailId.value)
  } finally {
    detailLoading.value = false
  }
}

async function onSaveDetail(payload: EcSalesOrderSaveRequest) {
  if (!detailId.value) return
  detailSaving.value = true
  try {
    await updateSalesOrder(detailId.value, payload)
    ElMessage.success(t('ecommerce.common.saved'))
    detailSaveCommitKey.value += 1
    await load()
    await loadDetail()
  } finally {
    detailSaving.value = false
  }
}

async function onDeleteDetailOrder() {
  if (!detail.value?.id) return
  if (detail.value.source !== 'MANUAL' && detail.value.status !== 'DRAFT') {
    ElMessage.warning(t('ecommerce.salesOrder.deleteNotAllowed'))
    return
  }
  const label = detail.value.platformOrderNo || detail.value.orderNo
  await ElMessageBox.confirm(t('ecommerce.salesOrder.deleteConfirm', { orderNo: label }), { type: 'warning' })
  deletingDetail.value = true
  try {
    await deleteSalesOrder(detail.value.id)
    ElMessage.success(t('ecommerce.common.deleted'))
    detailVisible.value = false
    detail.value = null
    detailId.value = null
    await load()
  } finally {
    deletingDetail.value = false
  }
}

async function onSave() {
  if (!form.shopId) {
    ElMessage.warning(t('ecommerce.salesOrder.shopRequired'))
    return
  }
  if (!form.orderTime) {
    ElMessage.warning(t('ecommerce.salesOrder.orderTimeRequired'))
    return
  }
  const lines = form.lines.filter((l) => l.linkName?.trim() && l.skuSpecName?.trim())
  if (!lines.length) {
    ElMessage.warning(t('ecommerce.salesOrder.linesRequired'))
    return
  }
  saving.value = true
  try {
    const payload = {
      shopId: form.shopId,
      expressStationId: form.expressStationId ?? null,
      orderTime: form.orderTime,
      payTime: form.payTime || form.orderTime,
      platformStatus: form.platformStatus || undefined,
      platformOrderNo: form.platformOrderNo || undefined,
      receivedAmount: form.receivedAmount ?? null,
      trackingNumber: form.trackingNumber || undefined,
      receiveAddress: form.receiveAddress || undefined,
      receiveProvince: form.receiveProvince || undefined,
      buyerRemark: form.buyerRemark || undefined,
      sellerRemark: form.sellerRemark || undefined,
      lines: lines.map((l, i) => ({
        listingLinkSkuId: l.listingLinkSkuId ?? null,
        linkName: l.linkName!.trim(),
        skuSpecName: l.skuSpecName!.trim(),
        skuQuantity: l.skuQuantity ?? 1,
        lineReceivedAmount: l.lineReceivedAmount ?? null,
        sortOrder: i,
      })),
    }
    if (editingId.value) {
      await updateSalesOrder(editingId.value, payload)
    } else {
      await createSalesOrder(payload)
    }
    ElMessage.success(t('ecommerce.common.saved'))
    dialogVisible.value = false
    editingId.value = null
    await load()
  } finally {
    saving.value = false
  }
}

function resetImport() {
  importPreview.value = null
  importFile.value = null
  parsedSpreadsheet.value = null
  importProfileId.value = null
  importProfileName.value = null
  statusMappingExpanded.value = false
  detectedColumnsExpanded.value = false
  reparsing.value = false
  importFileParsing.value = false
  manualCostDrafts.value = {}
  importExcludeStatuses.value = []
  importReviewSearch.value = ''
  importReviewBatchCost.value = ''
  pendingResumeBatchId.value = null
  importResumed.value = false
  importResumedFileName.value = null
  importUploadRef.value?.clearFiles()
}

function onImportShopChange() {
  if (importResumed.value) return
  importProfileId.value = null
  importProfileName.value = null
  importPreview.value = null
  void loadImportProfileForPlatform()
}

async function onImportOpen() {
  if (pendingResumeBatchId.value) {
    await resumePendingBatch(pendingResumeBatchId.value)
    pendingResumeBatchId.value = null
    return
  }
  if (importShopId.value) {
    await loadImportProfileForPlatform()
  }
}

async function loadImportProfileForPlatform() {
  if (!importPlatformId.value) {
    importProfileId.value = null
    importProfileName.value = null
    return
  }
  const profiles = await fetchImportProfiles(BIZ_SALES_ORDER, importPlatformId.value)
  const preferredName = defaultPlatformProfileName(importPlatformName.value || '')
  const preferred = profiles.find((p) => p.name === preferredName) ?? profiles[0]
  if (preferred?.id) {
    importProfileId.value = preferred.id
    importProfileName.value = preferred.name ?? null
  } else {
    importProfileId.value = null
    importProfileName.value = null
  }
}

function openMapping() {
  if (!importPlatformId.value) {
    ElMessage.warning(t('ecommerce.salesOrder.importShopRequired'))
    return
  }
  mappingVisible.value = true
}

async function onImportUploadChange(uploadFile: UploadFile) {
  const file = uploadFile.raw
  if (!file) return
  await handleImportFileSelected(file)
}

async function handleImportFileSelected(file: File) {
  if (!importShopId.value) {
    ElMessage.warning(t('ecommerce.salesOrder.importShopRequired'))
    importUploadRef.value?.clearFiles()
    return
  }
  importFile.value = file
  importResumedFileName.value = file.name
  importPreview.value = null
  importFileParsing.value = true
  detectedColumnsExpanded.value = false
  try {
    await ecSettings.ensureLoaded()
    parsedSpreadsheet.value = await detectSpreadsheetColumns(file, ecSettings.orderImport.headerRow)
    if (parsedSpreadsheet.value) {
      parsedSpreadsheet.value.dataStartRow = ecSettings.orderImport.dataStartRow
    }
    if (!parsedSpreadsheet.value.columns.length) {
      ElMessage.warning(t('ecommerce.salesOrder.importEmpty'))
      importFile.value = null
      importResumedFileName.value = null
      parsedSpreadsheet.value = null
      importUploadRef.value?.clearFiles()
      return
    }
  } catch {
    ElMessage.error(t('ecommerce.salesOrder.parseFailed'))
    importFile.value = null
    importResumedFileName.value = null
    parsedSpreadsheet.value = null
    importUploadRef.value?.clearFiles()
  } finally {
    importFileParsing.value = false
  }
}

function onMappingSaved(profile: SysImportProfile) {
  importProfileId.value = profile.id ?? null
  importProfileName.value = profile.name ?? defaultPlatformProfileName(importPlatformName.value || '')
  if (importPreview.value?.batchId) {
    void onReparseImport()
  }
}

function onStatusMappingSaved(profile: SysImportProfile) {
  importProfileId.value = profile.id ?? null
  importProfileName.value = profile.name ?? importProfileName.value
  if (importPreview.value?.batchId) {
    void onReparseImport()
  }
}

async function ensureImportProfile(): Promise<number> {
  if (importProfileId.value) {
    return importProfileId.value
  }
  await loadImportProfileForPlatform()
  if (importProfileId.value) {
    return importProfileId.value
  }
  if (!importPlatformId.value || !parsedSpreadsheet.value) {
    throw new Error('missing import context')
  }
  await ecSettings.ensureLoaded()
  const fields = filterImportFields(await fetchImportFields(BIZ_SALES_ORDER))
  const columnMapping = buildColumnMappingForUpload(
    fields,
    parsedSpreadsheet.value.columns,
    importPlatformName.value,
  )
  if (!columnMapping.link_name?.trim()) {
    ElMessage.warning(t('ecommerce.salesOrder.importMappingRequired'))
    throw new Error('link_name not mapped')
  }
  const profile = await createImportProfile({
    name: defaultPlatformProfileName(importPlatformName.value || ''),
    bizType: BIZ_SALES_ORDER,
    platformId: importPlatformId.value,
    fileType: parsedSpreadsheet.value.fileType,
    headerRow: parsedSpreadsheet.value.headerRow,
    dataStartRow: parsedSpreadsheet.value.dataStartRow,
    columnMapping,
    valueMapping: { ...ecSettings.statusMappingForImport },
  })
  importProfileId.value = profile.id ?? null
  importProfileName.value = profile.name ?? null
  return profile.id!
}

async function onFinishUpload() {
  if (!importFile.value || !importShopId.value || !parsedSpreadsheet.value) return
  uploading.value = true
  try {
    const profileId = await ensureImportProfile()
    importPreview.value = await uploadSalesOrderImport(
      importFile.value,
      importShopId.value,
      profileId,
      orderMonth.value,
    )
    importResumedFileName.value = importPreview.value.fileName ?? importFile.value.name
    ElMessage.success(t('ecommerce.salesOrder.uploadSuccess'))
  } finally {
    uploading.value = false
  }
}

function triggerImportFileReplace() {
  if (!importShopId.value) {
    ElMessage.warning(t('ecommerce.salesOrder.importShopRequired'))
    return
  }
  importReplaceInputRef.value?.click()
}

async function onImportReplaceFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (importPreview.value?.batchId) {
    reparsing.value = true
    importFile.value = file
    importResumedFileName.value = file.name
    try {
      importPreview.value = await replaceSalesOrderImportFile(importPreview.value.batchId, file)
      importResumedFileName.value = importPreview.value.fileName ?? file.name
      await ecSettings.ensureLoaded()
    parsedSpreadsheet.value = await detectSpreadsheetColumns(file, ecSettings.orderImport.headerRow)
    if (parsedSpreadsheet.value) {
      parsedSpreadsheet.value.dataStartRow = ecSettings.orderImport.dataStartRow
    }
      ElMessage.success(t('ecommerce.salesOrder.replaceFileSuccess'))
    } catch {
      ElMessage.error(t('ecommerce.salesOrder.replaceFileFailed'))
    } finally {
      reparsing.value = false
    }
    return
  }
  await handleImportFileSelected(file)
}

async function onReparseImport() {
  if (!importPreview.value?.batchId) return
  if (!canReparseImport.value) {
    ElMessage.warning(t('ecommerce.salesOrder.importFileMissingHint'))
    return
  }
  reparsing.value = true
  try {
    importPreview.value = await reparseSalesOrderImport(importPreview.value.batchId)
    ElMessage.success(t('ecommerce.salesOrder.reparseImportSuccess'))
  } catch {
    ElMessage.error(t('ecommerce.salesOrder.reparseImportFailed'))
  } finally {
    reparsing.value = false
  }
}

async function onCommitImport() {
  if (!importPreview.value?.batchId) return
  const reviewRows = importReviewRows.value
  const missingCost = reviewRows.filter((row) => {
    if (row.matchStatus !== 'UNMATCHED') return false
    const cost = resolveRowManualCost(row)
    return cost == null || cost === 0
  })
  if (missingCost.length) {
    ElMessage.warning(t('ecommerce.salesOrder.importUnmatchedCostRequired'))
    return
  }
  const missingStatus = reviewRows.filter(
    (row) => row.statusMatchStatus === 'UNMATCHED' && !row.lineStatus,
  )
  if (missingStatus.length) {
    ElMessage.warning(t('ecommerce.salesOrder.importUnmatchedStatusRequired'))
    return
  }
  importing.value = true
  try {
    const patches: ImportRowPatchItem[] = []
    for (const row of importPreview.value.rows) {
      if (row.id == null || row.parseStatus !== 'OK') continue
      if (isImportRowExcludedByStatus(row)) {
        const patch: ImportRowPatchItem = { rowId: row.id }
        if (row.matchStatus === 'UNMATCHED') {
          patch.manualCostPrice = 0
        }
        if (row.statusMatchStatus === 'UNMATCHED' && row.lineStatus) {
          patch.lineStatus = row.lineStatus
        }
        if (patch.manualCostPrice !== undefined || patch.lineStatus) {
          patches.push(patch)
        }
        continue
      }
      const needsReview = row.matchStatus === 'UNMATCHED' || row.statusMatchStatus === 'UNMATCHED'
      if (!needsReview) continue
      patches.push({
        rowId: row.id,
        ...(row.matchStatus === 'UNMATCHED'
          ? { manualCostPrice: resolveRowManualCost(row) ?? null }
          : {}),
        ...(row.statusMatchStatus === 'UNMATCHED'
          ? { lineStatus: row.lineStatus ?? null }
          : {}),
      })
    }
    await commitSalesOrderImport(importPreview.value.batchId, {
      ...(patches.length ? { items: patches } : {}),
      ...(importExcludeStatuses.value.length
        ? { excludedLineStatuses: [...importExcludeStatuses.value] }
        : {}),
    })
    ElMessage.success(t('ecommerce.salesOrder.importSuccess'))
    importVisible.value = false
    resetImport()
    await refreshAll()
  } catch {
    ElMessage.error(t('ecommerce.salesOrder.importCommitFailed'))
  } finally {
    importing.value = false
  }
}

onMounted(async () => {
  shopOptions.value = await fetchShopOptions()
  expressOptions.value = (await fetchExpressStations(undefined, { page: 1, pageSize: 100 })).records ?? []
  await refreshAll()
  openDetailFromRouteQuery()
})

watch(() => route.query.orderId, () => {
  openDetailFromRouteQuery()
})

watch(() => route.query.month, () => {
  applyMonthFromRouteQuery()
})

watch(orderMonth, () => {
  void refreshAll()
})

defineExpose({ load: refreshAll })
</script>

<style scoped lang="scss">
.sales-order-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: auto;
  gap: 16px;
}

.order-workbench-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 16px;
}

.order-workbench-header__main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-workbench-header__month {
  width: 160px;
}

.order-workbench-hint {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.order-section-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.order-stat-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.order-stat-card {
  padding: 14px 16px;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: #fff;

  &.is-blue {
    border-color: #dbeafe;
    background: linear-gradient(180deg, #f8fbff 0%, #fff 100%);
  }

  &.is-green {
    border-color: #bbf7d0;
    background: linear-gradient(180deg, #f6fff9 0%, #fff 100%);
  }

  &.is-orange {
    border-color: #fed7aa;
    background: linear-gradient(180deg, #fffaf5 0%, #fff 100%);
  }

  &.is-gray {
    border-color: #e5e7eb;
    background: linear-gradient(180deg, #fafafa 0%, #fff 100%);
  }
}

.order-stat-card__label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.order-stat-card__value {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--el-text-color-primary);
}

.order-stat-card__hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.order-shop-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.order-shop-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 14px 16px;
  border: 1px solid #e8ecf2;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    box-shadow: 0 4px 14px rgba(15, 23, 42, 0.06);
  }

  &.is-active {
    border-color: var(--el-color-primary);
    box-shadow: 0 0 0 1px color-mix(in srgb, var(--el-color-primary) 20%, transparent);
  }

  &.is-green {
    border-color: #bbf7d0;
  }

  &.is-orange {
    border-color: #fdba74;
  }

  &.is-gray {
    border-style: dashed;
    border-color: #d1d5db;
  }
}

.order-shop-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.order-shop-card__name-wrap,
.order-shop-card__platform-wrap {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.order-shop-card__name-wrap {
  flex: 1;
}

.order-shop-card__platform-wrap {
  flex-shrink: 0;
}

.order-shop-card__shop-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  flex-shrink: 0;
  object-fit: cover;
  background: #f3f4f6;

  &:not(.is-avatar) {
    object-fit: contain;
    padding: 2px;
  }
}

.order-shop-card__platform-avatar {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  flex-shrink: 0;
  object-fit: cover;
  background: #f3f4f6;

  &:not(.is-avatar) {
    object-fit: contain;
    padding: 2px;
  }
}

.order-shop-card__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-shop-card__platform {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.order-shop-card__status {
  margin: 12px 0 0;
  font-size: 13px;
  line-height: 1.65;
  color: var(--el-text-color-regular);
}

.order-shop-card__date {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.55;
  color: var(--el-text-color-secondary);
}

.order-shop-card__footer {
  margin-top: auto;
  padding-top: 14px;
  width: 100%;
  display: flex;
  justify-content: center;
}

.order-shop-card__action {
  height: 32px;
  padding: 0 18px;
  line-height: 1;
}

.order-list-section {
  flex: 1;
  min-height: 280px;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.panel-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.panel-toolbar__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.panel-search-input {
  width: 320px;
  flex-shrink: 0;
}

.order-time-range {
  width: 260px;
}

.import-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);

  &--block {
    display: block;
    margin-top: 8px;
    line-height: 1.5;
  }

  &--warn {
    color: var(--el-color-warning);
  }
}

.import-dialog {
  :deep(.el-dialog__body) {
    padding: 12px 20px 16px;
  }

  :deep(.el-dialog__footer) {
    padding-top: 12px;
  }
}

.import-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 480px;
}

.import-dialog-layout {
  display: flex;
  flex: 1;
  min-height: 420px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
}

.import-dialog-config {
  width: 340px;
  flex-shrink: 0;
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-right: 1px solid var(--el-border-color-lighter);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.import-config-section {
  flex-shrink: 0;

  &--file {
    margin-top: 8px;
  }
}

.import-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 700;
  color: #111;

  &::before {
    content: '';
    width: 3px;
    height: 14px;
    border-radius: 2px;
    background: var(--el-color-primary);
    flex-shrink: 0;
  }
}

.import-platform-tag {
  --el-tag-bg-color: #fff7ed;
  --el-tag-border-color: #fdba74;
  --el-tag-text-color: #ea580c;
}

.import-file-replace-input {
  display: none;
}

.import-file-card--parsing {
  cursor: default;
  pointer-events: none;
}

.import-file-card__parsing {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 72px;
  padding: 12px 0 4px;
}

.import-file-card__spinner {
  font-size: 28px;
  color: var(--el-color-primary);
}

.import-file-card__parsing-text {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.import-file-card__parsing-name {
  margin: 0;
  max-width: 100%;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  word-break: break-all;
  text-align: center;
}

.import-file-card--clickable {
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    box-shadow: 0 0 0 1px var(--el-color-primary-light-8);
  }
}

.import-config-toolbar {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.import-toolbar-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.import-start-parse-btn {
  width: 100%;
  min-height: 36px;
}

.import-reparse-btn {
  width: 100%;
  min-height: 36px;
}

.import-toolbar-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  background: var(--el-bg-color);
  color: var(--el-text-color-regular);
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.15s ease, background-color 0.15s ease;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    color: var(--el-text-color-primary);
  }

  &--active {
    border-color: var(--el-color-primary-light-5);
    background: var(--el-color-primary-light-9);
  }

  .el-icon {
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }
}

.import-toolbar-panel {
  margin-top: 8px;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.import-dialog-review {
  flex: 1;
  min-width: 0;
  padding: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--el-bg-color);
}

.import-review-banner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
}

.import-review-banner__icon {
  flex-shrink: 0;
  margin-top: 2px;
  font-size: 16px;
  color: #2563eb;
}

.import-review-banner__text {
  margin: 0;
  font-size: 13px;
  line-height: 1.55;
  color: #111;

  strong {
    font-weight: 700;
    color: #1d4ed8;
  }
}

.import-stats-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.import-stat-card {
  border-radius: 8px;
  padding: 12px 10px;
  text-align: center;
}

.import-stat-card__label {
  margin: 0 0 6px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.4;
}

.import-stat-card__value {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.import-stat-card--matched {
  background: #eff6ff;

  .import-stat-card__value {
    color: #2563eb;
  }
}

.import-stat-card--unmatched {
  background: #fef2f2;

  .import-stat-card__value {
    color: #dc2626;
  }
}

.import-stat-card--status {
  background: #fff7ed;

  .import-stat-card__value {
    color: #ea580c;
  }
}

.import-stat-card--error {
  background: #fef2f2;

  .import-stat-card__value {
    color: #dc2626;
  }
}

.import-panel-title {
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.import-review-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.import-panel-title--inline {
  margin-bottom: 0;
}

.import-review-exclude {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.import-review-exclude__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.import-review-exclude__select {
  width: 240px;
}

.import-required-mark {
  margin-left: 2px;
  color: var(--el-color-danger);
}

.import-platform-item {
  margin-bottom: 12px;

  :deep(.el-form-item__content) {
    display: none;
    height: 0;
    min-height: 0;
    margin: 0;
    padding: 0;
  }

  :deep(.el-form-item__label) {
    padding-bottom: 0;
    margin-bottom: 0;
    line-height: 1.5;
  }
}

.import-platform-inline {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.import-file-card {
  position: relative;
  width: 100%;
  min-height: 72px;
  padding: 12px 12px 28px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.import-file-card__main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 36px;
}

.import-file-type-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: -0.02em;

  &--xlsx,
  &--xls {
    background: #217346;
    font-size: 18px;
    font-weight: 700;

    &::before {
      content: 'X';
    }
  }

  &--csv {
    background: #0284c7;

    &::before {
      content: 'CSV';
    }
  }
}

.import-file-card__name {
  flex: 1;
  min-width: 0;
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  word-break: break-all;
  line-height: 1.35;
}

.import-file-card__check {
  flex-shrink: 0;
  font-size: 22px;
  color: var(--el-color-success);
}

.import-file-card__meta {
  position: absolute;
  right: 12px;
  bottom: 8px;
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
  text-align: right;
}

.import-mapping-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.import-profile-tag {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}

.import-form {
  :deep(.el-form-item) {
    margin-bottom: 14px;
  }

  :deep(.el-form-item__label) {
    padding-bottom: 4px;
    line-height: 1.4;
    font-weight: 500;
  }
}

.import-all-matched {
  margin-bottom: 8px;
  color: var(--el-color-success);
}

.import-review-table-wrap {
  flex: 1;
  min-height: 200px;
  height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 10px;

  :deep(.import-review-table) {
    flex: 1;
    min-height: 0;

    .el-table__header-wrapper th.el-table__cell .cell {
      text-align: center;
      justify-content: center;
    }
  }
}

.import-review-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 14px;
  flex-shrink: 0;
}

.import-review-search {
  flex: 1;
  min-width: 220px;
  max-width: 360px;

  :deep(.el-input__wrapper) {
    min-height: 36px;
  }
}

.import-review-batch {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.import-review-batch__cost {
  width: 108px;
  min-height: 36px;

  .import-manual-cost__prefix {
    padding: 0 9px;
    font-size: 14px;
  }

  :deep(.el-input__wrapper) {
    min-height: 34px;
  }
}

.import-review-batch__btn {
  min-height: 36px;
  padding: 8px 16px;
}

.import-review-toolbar__hint {
  flex-basis: 100%;
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.import-review-search-empty {
  flex: 1;
  justify-content: center;
}

.import-line-status-cell {
  display: flex;
  justify-content: center;
  align-items: center;
}

.import-line-status-select {
  width: 100%;
  max-width: 118px;
}

.import-review-empty {
  flex: 1;
  justify-content: center;

  &--placeholder {
    opacity: 0.85;
  }
}

.import-dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 16px;
}

.import-footer-hint {
  margin: 0;
  font-size: 13px;
  color: var(--el-color-warning);
}

.import-dialog-footer__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-left: auto;
}

.import-file-block {
  width: 100%;
}

.import-upload {
  width: 100%;

  :deep(.el-upload) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    width: 100%;
    padding: 14px 10px;
  }
}

.import-upload__icon {
  font-size: 40px;
  color: var(--el-color-primary);
}

.import-upload__trigger,
.import-upload__name {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.4;
}

.import-upload__name {
  color: var(--el-color-primary);
  word-break: break-all;
}

.import-meta {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
}

.import-stats-unmatched {
  color: var(--el-color-danger);
  font-weight: 700;
}

.inline-hint {
  margin-left: 8px;
}

.profile-tag {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-color-success);
}

.manual-cost-input {
  width: 100%;
}

.import-line-status-tag {
  border: none;
}

.import-manual-cost {
  display: flex;
  align-items: stretch;
  width: 100%;
  border: 1px solid #fdba74;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;

  &__prefix {
    display: flex;
    align-items: center;
    flex-shrink: 0;
    padding: 0 8px;
    font-size: 13px;
    font-weight: 500;
    color: #ea580c;
    background: #fff7ed;
    border-right: 1px solid #fdba74;
  }

  &__input {
    flex: 1;
    min-width: 0;

    :deep(.el-input__wrapper) {
      padding-left: 8px;
      padding-right: 8px;
      box-shadow: none !important;
      background: transparent;
      border: none;
    }

    :deep(.el-input__inner) {
      text-align: left;
    }

    :deep(.el-input__wrapper.is-focus) {
      box-shadow: none !important;
    }
  }

  &:focus-within {
    border-color: #f97316;
    box-shadow: 0 0 0 1px rgb(249 115 22 / 20%);
  }
}

.import-collapse {
  margin-bottom: 12px;

  :deep(.el-collapse-item__header) {
    height: 36px;
    line-height: 36px;
    font-size: 13px;
    color: var(--el-text-color-regular);
    background: transparent;
    border-bottom: none;
  }

  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
  }

  :deep(.el-collapse-item__content) {
    padding-bottom: 4px;
  }
}

.import-collapse--nested {
  margin-top: 10px;
  margin-bottom: 0;
  width: 100%;

  :deep(.el-collapse) {
    width: 100%;
    border-top: none;
    border-bottom: none;
  }

  :deep(.el-collapse-item__header) {
    width: 100%;
    padding-left: 0;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  :deep(.el-collapse-item__wrap) {
    border-bottom: 1px solid var(--el-border-color-lighter);
  }
}

.import-columns-panel {
  padding: 10px 12px 12px;
}

.import-columns-panel__summary {
  margin: 0 0 10px;
  font-size: 12px;
  color: #111;
  line-height: 1.4;
}

.import-columns-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 220px;
  overflow-y: auto;
  padding-right: 2px;
}

.import-column-tag {
  display: inline-block;
  max-width: 100%;
  padding: 4px 8px;
  border-radius: 4px;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  font-size: 12px;
  line-height: 1.35;
  color: #111;
  word-break: break-all;
}

.import-columns-list {
  margin: 0;
  word-break: break-all;
}

.sales-order-table {
  flex: 1;
  min-height: 200px;

  :deep(.el-table__body tr) {
    cursor: pointer;
  }

  :deep(.el-table__row.is-selected > td.el-table__cell) {
    background: var(--el-color-primary-light-9) !important;
  }

  :deep(.el-table__row.is-selected > td.el-table__cell:first-child) {
    box-shadow: inset 3px 0 0 #2563eb;
  }
}

.order-list-shop-name {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.order-list-shop-name__avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  flex-shrink: 0;
  object-fit: cover;
  background: #f3f4f6;

  &:not(.is-avatar) {
    object-fit: contain;
    padding: 2px;
  }
}

.order-list-shop-name__text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.line-count-tag {
  margin-left: 6px;
  vertical-align: middle;
}

.copyable-text {
  color: var(--el-color-primary);
  cursor: pointer;
  word-break: break-all;

  &:hover {
    text-decoration: underline;
  }
}
</style>
