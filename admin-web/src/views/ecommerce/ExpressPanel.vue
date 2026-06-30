<template>
  <div class="express-panel">
    <div class="express-panel__toolbar panel-toolbar">
      <el-input
        v-model="keyword"
        :placeholder="t('ecommerce.express.searchPlaceholder')"
        clearable
        class="express-panel__search"
      />
      <el-select
        v-model="regionFilter"
        multiple
        filterable
        clearable
        collapse-tags
        collapse-tags-tooltip
        :placeholder="t('ecommerce.express.regionFilterPlaceholder')"
        class="express-panel__region-filter"
      >
        <el-option v-for="name in regionOptions" :key="name" :label="name" :value="name" />
      </el-select>
      <el-checkbox v-model="defaultOnly" class="express-panel__default-filter">
        {{ t('ecommerce.express.filterDefaultOnly') }}
      </el-checkbox>
      <div class="express-panel__toolbar-spacer" />
      <el-button type="primary" @click="openCreate">{{ t('ecommerce.express.add') }}</el-button>
    </div>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="records"
      stripe
      border
      row-key="id"
      class="express-station-table"
      :expand-row-keys="expandedRowKeys"
      :row-class-name="rowClassName"
      @expand-change="onExpandChange"
      @row-click="onRowClick"
    >
      <el-table-column type="expand">
        <template #default="{ row }">
          <div v-loading="isExpandLoading(row.id)" class="express-expand">
            <template v-if="getExpandDetail(row.id)">
              <section class="express-expand__section express-expand__basic">
                <div class="express-expand__identity">
                  <ExpressStationAvatar :station="getExpandDetail(row.id)" size="lg" />
                  <div>
                    <div class="express-expand__identity-name">{{ getExpandDetail(row.id)?.name }}</div>
                    <el-tag v-if="getExpandDetail(row.id)?.isDefault" size="small" class="express-default-tag">
                      {{ t('ecommerce.express.defaultYes') }}
                    </el-tag>
                  </div>
                </div>
                <h4 class="express-expand__title">{{ t('ecommerce.express.expandBasicInfo') }}</h4>
                <dl class="express-expand__info">
                  <div class="express-expand__info-row">
                    <dt>{{ t('ecommerce.express.contact') }}</dt>
                    <dd>{{ getExpandDetail(row.id)?.contact || '—' }}</dd>
                  </div>
                  <div class="express-expand__info-row">
                    <dt>{{ t('ecommerce.express.address') }}</dt>
                    <dd>{{ getExpandDetail(row.id)?.address || '—' }}</dd>
                  </div>
                  <div class="express-expand__info-row">
                    <dt>{{ t('ecommerce.express.labelPrice') }}</dt>
                    <dd>
                      {{
                        getExpandDetail(row.id)?.labelPrice != null
                          ? formatPrice(getExpandDetail(row.id)!.labelPrice)
                          : '—'
                      }}
                    </dd>
                  </div>
                  <div class="express-expand__info-row">
                    <dt>{{ t('ecommerce.express.nameAliases') }}</dt>
                    <dd>
                      <template v-if="getExpandDetail(row.id)?.nameAliases?.length">
                        <span
                          v-for="alias in getExpandDetail(row.id)!.nameAliases"
                          :key="alias"
                          class="express-alias-tag"
                          :style="aliasTagStyle(alias)"
                        >
                          <span class="express-alias-tag__text">{{ alias }}</span>
                        </span>
                      </template>
                      <span v-else>—</span>
                    </dd>
                  </div>
                </dl>
              </section>

              <section class="express-expand__section express-expand__prices">
                <h4 class="express-expand__title">{{ t('ecommerce.express.priceHeatmap') }}</h4>
                <div
                  v-if="filteredExpandPrices(getExpandDetail(row.id)).length"
                  class="express-expand__heatmap-wrap"
                >
                  <table class="express-heatmap">
                    <thead>
                      <tr>
                        <th class="express-heatmap__province">{{ t('ecommerce.express.province') }}</th>
                        <th
                          v-for="col in priceColumns"
                          :key="col.key"
                          class="express-heatmap__weight"
                        >
                          {{ col.label }}
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="priceRow in filteredExpandPrices(getExpandDetail(row.id))"
                        :key="priceRow.id"
                      >
                        <td class="express-heatmap__province">{{ priceRow.provinceName }}</td>
                        <td
                          v-for="col in priceColumns"
                          :key="col.key"
                          class="express-heatmap__cell"
                          :style="priceHeatStyle(priceRow[col.key], filteredExpandPrices(getExpandDetail(row.id)))"
                        >
                          {{ formatPrice(priceRow[col.key]) }}
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <el-empty
                  v-else-if="regionFilter.length && getExpandDetail(row.id)?.prices?.length"
                  :description="t('ecommerce.express.noMatchedRegions')"
                  :image-size="56"
                />
                <el-empty v-else :description="t('ecommerce.express.noPrices')" :image-size="56" />
              </section>

              <section class="express-expand__section express-expand__notices">
                <h4 class="express-expand__title">
                  {{ t('ecommerce.express.noticesSection') }}
                  <el-tag v-if="getExpandDetail(row.id)?.notices?.length" size="small" type="info">
                    {{ getExpandDetail(row.id)!.notices!.length }}
                  </el-tag>
                </h4>
                <ol v-if="getExpandDetail(row.id)?.notices?.length" class="express-expand__notice-list">
                  <li
                    v-for="item in getExpandDetail(row.id)!.notices"
                    :key="item.id"
                    :class="{ 'text-red': item.highlightRed }"
                  >
                    {{ item.content }}
                  </li>
                </ol>
                <el-empty v-else :description="t('ecommerce.express.noNotices')" :image-size="56" />
              </section>
            </template>
            <div v-else-if="!isExpandLoading(row.id)" class="express-expand__empty">
              {{ t('ecommerce.express.loadingDetail') }}
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column :label="t('ecommerce.express.name')" min-width="160">
        <template #default="{ row }">
          <div class="express-station-name">
            <ExpressStationAvatar :station="row" size="md" />
            <span class="express-station-name__text">{{ row.name }}</span>
            <el-tag v-if="row.isDefault" size="small" class="express-default-tag">
              {{ t('ecommerce.express.defaultYes') }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="contact" :label="t('ecommerce.express.contact')" width="130" show-overflow-tooltip />
      <el-table-column prop="address" :label="t('ecommerce.express.address')" min-width="200" show-overflow-tooltip />
      <el-table-column :label="t('ecommerce.express.labelPrice')" width="100" align="right">
        <template #default="{ row }">
          <CnyAmount v-if="row.labelPrice != null" :value="row.labelPrice" :symbol="false" />
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.express.provinceCount')" width="88" align="center">
        <template #default="{ row }">
          <el-tag v-if="regionCount(row) > 0" size="small" type="info" effect="plain">
            {{ regionCount(row) }}
          </el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.express.noticeCount')" width="72" align="center">
        <template #default="{ row }">
          <el-tag v-if="noticeItemCount(row) > 0" size="small" type="warning" effect="plain">
            {{ noticeItemCount(row) }}
          </el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.express.updatedAt')" width="170">
        <template #default="{ row }">{{ formatDate(row.updateTime) }}</template>
      </el-table-column>
      <el-table-column
        :label="t('ecommerce.express.actions')"
        width="120"
        fixed="right"
        align="center"
        :class-name="TABLE_ACTIONS_CELL_CLASS"
      >
        <template #default="{ row }">
          <div class="table-actions-cell-inner" @click.stop>
            <el-button link type="primary" :title="t('ecommerce.express.edit')" @click.stop="openEdit(row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button link type="primary" :title="t('ecommerce.express.copyStation')" @click.stop="onCopy(row)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
            <el-button link type="danger" :title="t('ecommerce.express.delete')" @click.stop="onDelete(row)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <TablePagination
      :page="page"
      :page-size="pageSize"
      :total="total"
      @update:page="onPageChange"
      @update:page-size="onSizeChange"
    />

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? t('ecommerce.express.editTitle') : t('ecommerce.express.createTitle')"
      width="1120px"
      destroy-on-close
      top="4vh"
      class="express-edit-dialog"
    >
      <div class="express-edit">
        <div class="express-edit__layout">
          <aside class="express-edit__left">
            <section class="express-edit__card express-edit__card--basic">
              <div class="express-edit__profile">
                <EcImageField
                  v-model="form.avatarUrl"
                  size="large"
                  :dialog-title="t('ecommerce.express.avatar')"
                />
                <div class="express-edit__profile-meta">
                  <div class="express-edit__profile-title">
                    <span>{{ form.name.trim() || t('ecommerce.express.createTitle') }}</span>
                    <el-tag v-if="form.isDefault" size="small" class="express-default-tag">
                      {{ t('ecommerce.express.defaultYes') }}
                    </el-tag>
                  </div>
                  <p class="express-edit__profile-hint">{{ t('ecommerce.express.avatarHint') }}</p>
                </div>
              </div>

              <div class="express-edit__card-head">
                <h4 class="express-edit__card-title">{{ t('ecommerce.express.tabBasic') }}</h4>
                <el-button class="express-save-btn" :loading="savingBasic" @click="onSaveBasicSection">
                  {{ t('ecommerce.express.saveBasicSection') }}
                </el-button>
              </div>

              <el-form :model="form" label-width="88px" class="express-edit__form">
                <el-form-item :label="t('ecommerce.express.name')" required>
                  <el-input v-model="form.name" />
                </el-form-item>
                <el-form-item :label="t('ecommerce.express.contact')">
                  <el-input v-model="form.contact" />
                </el-form-item>
                <el-form-item :label="t('ecommerce.express.address')">
                  <el-input v-model="form.address" type="textarea" :rows="3" />
                </el-form-item>
                <el-form-item :label="t('ecommerce.express.labelPrice')">
                  <el-input-number
                    v-model="form.labelPrice"
                    :min="0"
                    :precision="2"
                    :step="0.1"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
                <el-form-item :label="t('ecommerce.express.isDefault')">
                  <el-switch v-model="form.isDefault" class="express-default-switch" />
                </el-form-item>
                <el-form-item>
                  <template #label>
                    <span class="express-form-label">
                      {{ t('ecommerce.express.nameAliases') }}
                      <el-tooltip
                        :content="t('ecommerce.express.nameAliasesHint')"
                        placement="top"
                        :show-after="200"
                      >
                        <el-icon class="express-field-tip"><InfoFilled /></el-icon>
                      </el-tooltip>
                    </span>
                  </template>
                  <div class="express-alias-tags">
                    <span
                      v-for="(alias, index) in form.nameAliases"
                      :key="`${alias}-${index}`"
                      class="express-alias-tag"
                      :style="aliasTagStyle(alias)"
                    >
                      <span class="express-alias-tag__text">{{ alias }}</span>
                      <button
                        type="button"
                        class="express-alias-tag__remove"
                        :title="t('ecommerce.express.delete')"
                        @click="removeNameAlias(index)"
                      >
                        <el-icon><Close /></el-icon>
                      </button>
                    </span>
                    <el-popover
                      v-model:visible="aliasInputVisible"
                      placement="bottom-start"
                      :width="280"
                      trigger="click"
                      @show="focusAliasInput"
                    >
                      <template #reference>
                        <button type="button" class="express-alias-add">
                          <el-icon><Plus /></el-icon>
                          {{ t('ecommerce.express.addNameAlias') }}
                        </button>
                      </template>
                      <div class="express-alias-popover">
                        <el-input
                          ref="aliasInputRef"
                          v-model="nameAliasInput"
                          :placeholder="t('ecommerce.express.nameAliasesPlaceholder')"
                          @keydown.enter.prevent="onAddNameAliasFromPopover"
                        />
                        <div class="express-alias-popover__actions">
                          <el-button size="small" @click="aliasInputVisible = false">
                            {{ t('ecommerce.common.cancel') }}
                          </el-button>
                          <el-button size="small" class="express-save-btn" @click="onAddNameAliasFromPopover">
                            {{ t('ecommerce.express.addNameAlias') }}
                          </el-button>
                        </div>
                      </div>
                    </el-popover>
                  </div>
                </el-form-item>
              </el-form>
            </section>
          </aside>

          <div class="express-edit__right">
            <section class="express-edit__card express-edit__card--prices">
              <div class="express-edit__card-head">
                <h4 class="express-edit__card-title">
                  {{ t('ecommerce.express.priceHeatmap') }}
                  <el-tag v-if="prices.length" size="small" type="info">{{ prices.length }}</el-tag>
                </h4>
                <div class="express-edit__card-actions">
                  <el-button class="express-save-btn" :loading="savingPriceSection" :disabled="!editingId" @click="onSavePriceSection">
                    {{ t('ecommerce.express.savePriceSection') }}
                  </el-button>
                  <el-button type="primary" size="small" :disabled="!editingId" @click="openPriceCreate">
                    {{ t('ecommerce.express.addPrice') }}
                  </el-button>
                </div>
              </div>
              <div v-if="!editingId" class="tab-hint">{{ t('ecommerce.express.saveStationFirst') }}</div>
              <template v-else>
                <div v-if="prices.length" class="express-edit__price-toolbar">
                  <el-input
                    v-model="priceRegionKeyword"
                    class="express-edit__price-search"
                    :placeholder="t('ecommerce.express.priceRegionSearchPlaceholder')"
                    clearable
                    :prefix-icon="Search"
                    @input="onPriceRegionSearchInput"
                  />
                </div>
                <div v-if="prices.length && filteredEditPrices.length" class="express-edit__heatmap-wrap">
                <table class="express-heatmap">
                  <thead>
                    <tr>
                      <th class="express-heatmap__province">{{ t('ecommerce.express.province') }}</th>
                      <th v-for="col in priceColumns" :key="col.key" class="express-heatmap__weight">
                        {{ col.label }}
                      </th>
                      <th class="express-heatmap__actions">{{ t('ecommerce.express.actions') }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="priceRow in filteredEditPrices" :key="priceRow.id">
                      <td class="express-heatmap__province">{{ priceRow.provinceName }}</td>
                      <td
                        v-for="col in priceColumns"
                        :key="col.key"
                        class="express-heatmap__cell"
                        :style="priceHeatStyle(priceRow[col.key], filteredEditPrices)"
                      >
                        {{ formatPrice(priceRow[col.key]) }}
                      </td>
                      <td class="express-heatmap__actions">
                        <div class="table-actions-cell-inner" @click.stop>
                          <el-button link type="primary" :title="t('ecommerce.express.edit')" @click.stop="openPriceEdit(priceRow)">
                            <el-icon><Edit /></el-icon>
                          </el-button>
                          <el-button link type="primary" :title="t('ecommerce.express.copyPrice')" @click.stop="openPriceCopy(priceRow)">
                            <el-icon><CopyDocument /></el-icon>
                          </el-button>
                          <el-button link type="danger" :title="t('ecommerce.express.delete')" @click.stop="onDeletePrice(priceRow)">
                            <el-icon><Delete /></el-icon>
                          </el-button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
                </div>
                <div v-else-if="prices.length && priceRegionKeyword.trim()" class="tab-hint">
                  {{ t('ecommerce.express.noMatchedPriceRegions') }}
                </div>
                <el-empty v-else :description="t('ecommerce.express.noPrices')" :image-size="56" />
              </template>
            </section>

            <section class="express-edit__card express-edit__card--notices">
              <div class="express-edit__card-head">
                <h4 class="express-edit__card-title">
                  {{ t('ecommerce.express.noticesSection') }}
                  <el-tag v-if="notices.length" size="small" type="warning">{{ notices.length }}</el-tag>
                </h4>
                <div class="express-edit__card-actions">
                  <el-button class="express-save-btn" :loading="savingNoticeSection" :disabled="!editingId" @click="onSaveNoticeSection">
                    {{ t('ecommerce.express.saveNoticeSection') }}
                  </el-button>
                  <el-button type="primary" size="small" :disabled="!editingId" @click="openNoticeCreate">
                    {{ t('ecommerce.express.addNotice') }}
                  </el-button>
                </div>
              </div>
              <div v-if="!editingId" class="tab-hint">{{ t('ecommerce.express.saveStationFirst') }}</div>
              <el-table
                v-else
                ref="noticeTableRef"
                v-loading="noticeReordering"
                :data="notices"
                row-key="id"
                stripe
                border
                size="small"
                class="express-edit__notice-table"
              >
                <el-table-column width="44" align="center" class-name="express-notice-drag-col">
                  <template #default>
                    <div class="express-notice-drag-cell">
                      <el-icon class="express-notice-drag-handle" :title="t('ecommerce.express.noticeDragHint')">
                        <Rank />
                      </el-icon>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="sortOrder" :label="t('ecommerce.express.sortOrder')" width="70" align="center" />
                <el-table-column :label="t('ecommerce.express.noticeContent')" min-width="280" align="center">
                  <template #default="{ row }">
                    <span :class="{ 'text-red': row.highlightRed }">{{ row.content }}</span>
                  </template>
                </el-table-column>
                <el-table-column
                  :label="t('ecommerce.express.actions')"
                  width="88"
                  align="center"
                  :class-name="TABLE_ACTIONS_CELL_CLASS"
                >
                  <template #default="{ row }">
                    <div class="table-actions-cell-inner" @click.stop>
                      <el-button link type="primary" :title="t('ecommerce.express.edit')" @click.stop="openNoticeEdit(row)">
                        <el-icon><Edit /></el-icon>
                      </el-button>
                      <el-button link type="danger" :title="t('ecommerce.express.delete')" @click.stop="onDeleteNotice(row)">
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </section>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button class="express-save-btn" :loading="savingAll" @click="onSaveAll">
          {{ t('ecommerce.express.saveAll') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="priceDialogVisible"
      :title="priceEditingId ? t('ecommerce.express.editPriceTitle') : t('ecommerce.express.addPriceTitle')"
      width="720px"
      destroy-on-close
      append-to-body
      class="express-price-dialog"
    >
      <div class="express-price-dialog__cards">
        <section class="express-price-dialog__card">
          <h4 class="express-price-dialog__section-title">{{ t('ecommerce.express.selectRegion') }}</h4>
          <el-select
            v-model="priceForm.provinceName"
            filterable
            allow-create
            default-first-option
            :reserve-keyword="false"
            :placeholder="t('ecommerce.express.selectRegionPlaceholder')"
            style="width: 100%"
          >
            <el-option v-for="name in regionOptions" :key="name" :label="name" :value="name" />
          </el-select>
          <div v-if="recentPriceRegions.length" class="express-price-dialog__recent">
            <span class="express-price-dialog__recent-label">{{ t('ecommerce.express.recentRegions') }}</span>
            <button
              v-for="name in recentPriceRegions"
              :key="name"
              type="button"
              class="express-price-dialog__recent-tag"
              @click="selectRecentRegion(name)"
            >
              {{ name }}
            </button>
          </div>
        </section>

        <section class="express-price-dialog__card">
          <div class="express-price-dialog__section-head">
            <h4 class="express-price-dialog__section-title express-price-dialog__section-title--inline">
              {{ t('ecommerce.express.weightPriceSection') }}
            </h4>
            <el-button
              v-if="!priceEditingId"
              link
              type="primary"
              class="express-price-dialog__copy-link"
              @click="copyPreviousRegionPrice"
            >
              <el-icon><CopyDocument /></el-icon>
              {{ t('ecommerce.express.copyPreviousRegionPrice') }}
            </el-button>
          </div>
          <el-collapse v-model="priceCollapseActive" class="express-price-dialog__collapse">
            <el-collapse-item
              v-for="group in priceWeightGroups"
              :key="group.name"
              :name="group.name"
              :title="group.title"
            >
              <div class="express-price-dialog__price-grid">
                <div v-for="key in group.keys" :key="key" class="express-price-dialog__price-field">
                  <label class="express-price-dialog__price-label">{{ getPriceColumnLabel(key) }}</label>
                  <el-input-number
                    v-model="priceForm[key]"
                    class="express-price-dialog__price-input"
                    :min="0"
                    :precision="2"
                    :step="0.5"
                    controls-position="right"
                  />
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </section>
      </div>

      <template #footer>
        <el-button @click="priceDialogVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button class="express-save-btn" :loading="priceSaving" @click="onSavePrice">
          {{ t('ecommerce.common.save') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="noticeDialogVisible"
      :title="noticeEditingId ? t('ecommerce.express.editNoticeTitle') : t('ecommerce.express.addNoticeTitle')"
      width="640px"
      destroy-on-close
      append-to-body
      class="express-notice-dialog"
    >
      <div class="express-notice-dialog__body">
        <el-form :model="noticeForm" label-position="top" class="express-notice-dialog__form">
          <el-form-item :label="t('ecommerce.express.noticeContent')" required>
            <el-input
              v-model="noticeForm.content"
              type="textarea"
              :rows="4"
              maxlength="200"
              show-word-limit
              :placeholder="t('ecommerce.express.noticeContentPlaceholder')"
            />
          </el-form-item>
          <el-form-item>
            <template #label>
              <span class="express-form-label">
                {{ t('ecommerce.express.noticeHighlightLabel') }}
                <el-tooltip
                  :content="t('ecommerce.express.noticeHighlightHint')"
                  placement="top"
                  :show-after="200"
                >
                  <el-icon class="express-field-tip"><InfoFilled /></el-icon>
                </el-tooltip>
              </span>
            </template>
            <div class="express-notice-dialog__highlight-row">
              <el-switch v-model="noticeForm.highlightRed" class="express-notice-highlight-switch" />
              <span class="express-notice-dialog__highlight-hint">{{ t('ecommerce.express.noticeHighlightHint') }}</span>
            </div>
          </el-form-item>
          <el-form-item :label="t('ecommerce.express.sortOrder')">
            <el-input-number
              v-model="noticeForm.sortOrder"
              :min="1"
              :step="1"
              controls-position="right"
              style="width: 160px"
            />
            <div class="express-notice-dialog__sort-hint">{{ t('ecommerce.express.noticeSortHint') }}</div>
          </el-form-item>
        </el-form>

        <section class="express-notice-dialog__preview">
          <h4 class="express-notice-dialog__preview-title">{{ t('ecommerce.express.noticePreviewTitle') }}</h4>
          <div class="express-notice-preview-card">
            <el-icon class="express-notice-preview-card__drag"><Rank /></el-icon>
            <span
              class="express-notice-preview-card__sort"
              :class="{ 'text-red': noticeForm.highlightRed }"
            >
              {{ noticeForm.sortOrder }}
            </span>
            <span
              class="express-notice-preview-card__content"
              :class="{ 'text-red': noticeForm.highlightRed }"
            >
              {{ noticePreviewText }}
            </span>
          </div>
        </section>
      </div>
      <template #footer>
        <el-button @click="noticeDialogVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button class="express-save-btn" :loading="noticeSaving" @click="onSaveNotice">
          {{ t('ecommerce.common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type InputInstance, type TableInstance } from 'element-plus'
import type { TableColumnCtx } from 'element-plus'
import { Close, CopyDocument, Delete, Edit, InfoFilled, Plus, Rank, Search } from '@element-plus/icons-vue'
import CnyAmount from '@/components/CnyAmount.vue'
import { formatMoney as formatCnyPlain } from '@/utils/formatMoney'
import Sortable from 'sortablejs'
import {
  createExpressNotice,
  createExpressPrice,
  createExpressStation,
  copyExpressStation,
  deleteExpressNotice,
  deleteExpressPrice,
  deleteExpressStation,
  fetchExpressNotices,
  fetchExpressPrices,
  fetchExpressStation,
  fetchExpressStations,
  fetchExpressRegions,
  updateExpressNotice,
  updateExpressPrice,
  updateExpressStation,
  type EcExpressNotice,
  type EcExpressPrice,
  type EcExpressStation,
  type EcExpressStationSaveRequest,
} from '@/api/ecommerce/express'
import TablePagination from '@/components/TablePagination.vue'
import EcImageField from '@/components/ecommerce/EcImageField.vue'
import ExpressStationAvatar from '@/components/ecommerce/ExpressStationAvatar.vue'
import { usePagination } from '@/composables/usePagination'
import { TABLE_ACTIONS_CELL_CLASS } from '@/constants/table'
import { formatDate } from '@/utils/date'
import { aliasTagStyle } from '@/utils/expressVisual'

const { t } = useI18n()

const saving = ref(false)
const savingBasic = ref(false)
const savingPriceSection = ref(false)
const savingNoticeSection = ref(false)
const savingAll = ref(false)
const keyword = ref('')
const defaultOnly = ref(false)
const regionFilter = ref<string[]>([])
const regionOptions = ref<string[]>([])
const tableRef = ref<TableInstance>()
const noticeTableRef = ref<TableInstance>()
const expandedRowKeys = ref<number[]>([])
const expandDetails = ref(new Map<number, EcExpressStation>())
const expandLoadingIds = ref(new Set<number>())

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } = usePagination(
  (p, ps) =>
    fetchExpressStations(keyword.value.trim() || undefined, { page: p, pageSize: ps }, {
      defaultOnly: defaultOnly.value,
      regionNames: regionFilter.value.length ? regionFilter.value : undefined,
    }),
)

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const prices = ref<EcExpressPrice[]>([])
const notices = ref<EcExpressNotice[]>([])

const priceDialogVisible = ref(false)
const priceEditingId = ref<number | null>(null)
const priceSaving = ref(false)
const priceCollapseActive = ref(['le1'])

const noticeDialogVisible = ref(false)
const noticeEditingId = ref<number | null>(null)
const noticeSaving = ref(false)

const form = reactive({
  name: '',
  avatarUrl: '',
  contact: '',
  address: '',
  labelPrice: null as number | null,
  isDefault: false,
  nameAliases: [] as string[],
})

const nameAliasInput = ref('')
const aliasInputVisible = ref(false)
const aliasInputRef = ref<InputInstance>()
const priceRegionKeyword = ref('')
const noticeReordering = ref(false)
let noticeSortable: Sortable | null = null

type PriceFieldKey =
  | 'priceW03Kg'
  | 'priceW05Kg'
  | 'priceW1Kg'
  | 'priceW15Kg'
  | 'priceW2Kg'
  | 'priceW25Kg'
  | 'priceW3Kg'
  | 'over3FirstPrice'
  | 'over3AdditionalPrice'

const priceColumns = computed(() => [
  { key: 'priceW03Kg' as PriceFieldKey, label: t('ecommerce.express.w03') },
  { key: 'priceW05Kg' as PriceFieldKey, label: t('ecommerce.express.w05') },
  { key: 'priceW1Kg' as PriceFieldKey, label: t('ecommerce.express.w1') },
  { key: 'priceW15Kg' as PriceFieldKey, label: t('ecommerce.express.w15') },
  { key: 'priceW2Kg' as PriceFieldKey, label: t('ecommerce.express.w2') },
  { key: 'priceW25Kg' as PriceFieldKey, label: t('ecommerce.express.w25') },
  { key: 'priceW3Kg' as PriceFieldKey, label: t('ecommerce.express.w3') },
  { key: 'over3FirstPrice' as PriceFieldKey, label: t('ecommerce.express.over3First') },
  { key: 'over3AdditionalPrice' as PriceFieldKey, label: t('ecommerce.express.over3Additional') },
])

const priceWeightGroups = computed(() => [
  {
    name: 'le1',
    title: t('ecommerce.express.priceGroupLe1'),
    keys: ['priceW03Kg', 'priceW05Kg', 'priceW1Kg'] as PriceFieldKey[],
  },
  {
    name: 'mid',
    title: t('ecommerce.express.priceGroupMid'),
    keys: ['priceW15Kg', 'priceW2Kg', 'priceW25Kg', 'priceW3Kg'] as PriceFieldKey[],
  },
  {
    name: 'over3',
    title: t('ecommerce.express.priceGroupOver3'),
    keys: ['over3FirstPrice', 'over3AdditionalPrice'] as PriceFieldKey[],
  },
])

const recentPriceRegions = computed(() => {
  const names = prices.value.map((item) => item.provinceName.trim()).filter(Boolean)
  return [...new Set(names)].slice(-6).reverse()
})

function getPriceColumnLabel(key: PriceFieldKey) {
  return priceColumns.value.find((col) => col.key === key)?.label ?? key
}

function selectRecentRegion(name: string) {
  priceForm.provinceName = name
}

function copyPreviousRegionPrice() {
  if (!prices.value.length) {
    ElMessage.warning(t('ecommerce.express.noPreviousRegionPrice'))
    return
  }
  const previous = prices.value[prices.value.length - 1]
  priceColumns.value.forEach((col) => {
    priceForm[col.key] = previous[col.key] ?? null
  })
  ElMessage.success(t('ecommerce.express.copyPreviousRegionPriceSuccess'))
}

const priceForm = reactive<Record<PriceFieldKey, number | null> & { provinceName: string }>({
  provinceName: '',
  priceW03Kg: null,
  priceW05Kg: null,
  priceW1Kg: null,
  priceW15Kg: null,
  priceW2Kg: null,
  priceW25Kg: null,
  priceW3Kg: null,
  over3FirstPrice: null,
  over3AdditionalPrice: null,
})

const noticeForm = reactive({
  content: '',
  highlightRed: false,
  sortOrder: 0,
})

const noticePreviewText = computed(() => {
  const text = noticeForm.content.trim()
  return text || t('ecommerce.express.noticePreviewPlaceholder')
})

function formatPrice(value: number | null | undefined) {
  if (value == null) return '—'
  return formatCnyPlain(value, { symbol: false })
}

function rowClassName({ row }: { row: EcExpressStation }) {
  return expandedRowKeys.value.includes(row.id) ? 'express-station-row is-expanded' : 'express-station-row'
}

function getExpandDetail(id: number) {
  return expandDetails.value.get(id)
}

function filteredExpandPrices(detail?: EcExpressStation | null) {
  if (!detail?.prices?.length) return []
  if (!regionFilter.value.length) return detail.prices
  const selected = new Set(regionFilter.value.map((name) => name.trim()))
  return detail.prices.filter((price) => selected.has(price.provinceName.trim()))
}

const filteredEditPrices = computed(() => {
  const keyword = priceRegionKeyword.value.trim().toLowerCase()
  if (!keyword) return prices.value
  return prices.value.filter((price) => price.provinceName.toLowerCase().includes(keyword))
})

function onPriceRegionSearchInput(value: string) {
  priceRegionKeyword.value = value
}

/** 地区数 = 价格矩阵行数 */
function regionCount(row: EcExpressStation) {
  if (row.priceCount != null) {
    return row.priceCount
  }
  return getExpandDetail(row.id)?.prices?.length ?? 0
}

/** 须知 = 注意事项条数 */
function noticeItemCount(row: EcExpressStation) {
  if (row.noticeCount != null) {
    return row.noticeCount
  }
  return getExpandDetail(row.id)?.notices?.length ?? 0
}

function syncRowCounts(row: EcExpressStation, detail: EcExpressStation) {
  row.priceCount = detail.prices?.length ?? 0
  row.noticeCount = detail.notices?.length ?? 0
}

function isExpandLoading(id: number) {
  return expandLoadingIds.value.has(id)
}

function invalidateExpandDetail(id: number) {
  expandDetails.value.delete(id)
  expandDetails.value = new Map(expandDetails.value)
}

function collectPriceValues(priceRows: EcExpressPrice[]) {
  const values: number[] = []
  for (const row of priceRows) {
    for (const col of priceColumns.value) {
      const value = row[col.key]
      if (value != null && !Number.isNaN(Number(value))) {
        values.push(Number(value))
      }
    }
  }
  return values
}

function priceHeatStyle(value: number | null | undefined, priceRows: EcExpressPrice[]) {
  if (value == null) {
    return { background: 'transparent' }
  }
  const allValues = collectPriceValues(priceRows)
  if (!allValues.length) {
    return { background: 'var(--wr-stat-green-bg, #f0fdf4)' }
  }
  const min = Math.min(...allValues)
  const max = Math.max(...allValues)
  if (min === max) {
    return { background: 'var(--wr-stat-green-bg, #f0fdf4)' }
  }
  const ratio = (Number(value) - min) / (max - min)
  const low = [240, 253, 244]
  const high = [255, 247, 237]
  const r = Math.round(low[0] + (high[0] - low[0]) * ratio)
  const g = Math.round(low[1] + (high[1] - low[1]) * ratio)
  const b = Math.round(low[2] + (high[2] - low[2]) * ratio)
  return { background: `rgb(${r}, ${g}, ${b})` }
}

function resetForm() {
  form.name = ''
  form.avatarUrl = ''
  form.contact = ''
  form.address = ''
  form.labelPrice = null
  form.isDefault = false
  form.nameAliases = []
  nameAliasInput.value = ''
  aliasInputVisible.value = false
  priceRegionKeyword.value = ''
}

function destroyNoticeSortable() {
  noticeSortable?.destroy()
  noticeSortable = null
}

function setupNoticeSortable() {
  destroyNoticeSortable()
  if (!dialogVisible.value || !editingId.value || !notices.value.length) {
    return
  }
  void nextTick(() => {
    const tbody = noticeTableRef.value?.$el?.querySelector('.el-table__body-wrapper tbody')
    if (!tbody) return
    noticeSortable = Sortable.create(tbody as HTMLElement, {
      animation: 150,
      handle: '.express-notice-drag-handle',
      ghostClass: 'express-notice-row--ghost',
      disabled: noticeReordering.value,
      onEnd: (evt) => {
        void onNoticeReorder(evt.oldIndex, evt.newIndex)
      },
    })
  })
}

async function onNoticeReorder(oldIndex?: number, newIndex?: number) {
  if (
    oldIndex === undefined
    || newIndex === undefined
    || oldIndex === newIndex
    || !editingId.value
  ) {
    return
  }

  const ordered = [...notices.value]
  const [moved] = ordered.splice(oldIndex, 1)
  if (!moved) return
  ordered.splice(newIndex, 0, moved)

  const previous = new Map(notices.value.map((item) => [item.id, item.sortOrder ?? 0]))
  ordered.forEach((item, index) => {
    item.sortOrder = index
  })
  notices.value = [...ordered]

  const updates = ordered.filter((item, index) => previous.get(item.id) !== index)
  if (!updates.length) {
    return
  }

  noticeReordering.value = true
  try {
    await Promise.all(
      updates.map((item) =>
        updateExpressNotice(item.id, {
          stationId: editingId.value!,
          content: item.content,
          highlightRed: item.highlightRed,
          sortOrder: item.sortOrder,
        }),
      ),
    )
    invalidateExpandDetail(editingId.value!)
    if (expandedRowKeys.value.includes(editingId.value)) {
      await loadExpandDetail(editingId.value)
    }
  } catch {
    if (editingId.value) {
      await loadStationChildren(editingId.value)
    }
    ElMessage.error(t('ecommerce.express.noticeReorderFailed'))
  } finally {
    noticeReordering.value = false
    setupNoticeSortable()
  }
}

function addNameAlias(): boolean {
  const value = nameAliasInput.value.trim()
  if (!value) return false
  if (form.nameAliases.includes(value)) {
    nameAliasInput.value = ''
    return false
  }
  form.nameAliases.push(value)
  nameAliasInput.value = ''
  return true
}

function onAddNameAliasFromPopover() {
  if (addNameAlias()) {
    aliasInputVisible.value = false
  }
}

function focusAliasInput() {
  void nextTick(() => aliasInputRef.value?.focus())
}

function removeNameAlias(index: number) {
  form.nameAliases.splice(index, 1)
}

function resetPriceForm() {
  priceForm.provinceName = ''
  priceColumns.value.forEach((col) => {
    priceForm[col.key] = null
  })
}

function resetNoticeForm() {
  noticeForm.content = ''
  noticeForm.highlightRed = false
  noticeForm.sortOrder = notices.value.length + 1
}

async function loadStations() {
  await load()
}

async function loadStationChildren(stationId: number) {
  const [priceList, noticeList] = await Promise.all([
    fetchExpressPrices(stationId),
    fetchExpressNotices(stationId),
  ])
  prices.value = priceList
  notices.value = noticeList
}

async function loadExpandDetail(id: number) {
  if (expandDetails.value.has(id) || expandLoadingIds.value.has(id)) {
    return
  }
  const nextLoading = new Set(expandLoadingIds.value)
  nextLoading.add(id)
  expandLoadingIds.value = nextLoading
  try {
    const detail = await fetchExpressStation(id)
    const nextDetails = new Map(expandDetails.value)
    nextDetails.set(id, detail)
    expandDetails.value = nextDetails
    const row = records.value.find((item) => item.id === id)
    if (row) {
      syncRowCounts(row, detail)
    }
  } finally {
    const doneLoading = new Set(expandLoadingIds.value)
    doneLoading.delete(id)
    expandLoadingIds.value = doneLoading
  }
}

async function onExpandChange(row: EcExpressStation, expandedRows: EcExpressStation[]) {
  expandedRowKeys.value = expandedRows.map((item) => item.id)
  if (expandedRows.some((item) => item.id === row.id)) {
    await loadExpandDetail(row.id)
  }
}

function onRowClick(row: EcExpressStation, column: TableColumnCtx<EcExpressStation>) {
  if (column.className === TABLE_ACTIONS_CELL_CLASS || column.type === 'expand') {
    return
  }
  tableRef.value?.toggleRowExpansion(row)
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => load(true), 300)
})

watch(defaultOnly, () => {
  expandedRowKeys.value = []
  load(true)
})

watch(regionFilter, () => {
  expandedRowKeys.value = []
  expandDetails.value = new Map()
  load(true)
})

watch(
  [dialogVisible, () => notices.value.length, editingId],
  ([visible]) => {
    if (visible && editingId.value) {
      setupNoticeSortable()
      return
    }
    destroyNoticeSortable()
  },
)

function openCreate() {
  editingId.value = null
  resetForm()
  prices.value = []
  notices.value = []
  dialogVisible.value = true
}

async function openEdit(row: EcExpressStation) {
  editingId.value = row.id
  const detail = await fetchExpressStation(row.id)
  form.name = detail.name
  form.avatarUrl = detail.avatarUrl || ''
  form.contact = detail.contact || ''
  form.address = detail.address || ''
  form.labelPrice = detail.labelPrice ?? null
  form.isDefault = !!detail.isDefault
  form.nameAliases = [...(detail.nameAliases || [])]
  nameAliasInput.value = ''
  aliasInputVisible.value = false
  priceRegionKeyword.value = ''
  prices.value = detail.prices || []
  notices.value = detail.notices || []
  dialogVisible.value = true
}

async function onSaveStation(options?: { silent?: boolean; loadingRef?: typeof savingBasic }) {
  if (!form.name.trim()) {
    ElMessage.warning(t('ecommerce.express.nameRequired'))
    return false
  }

  const loader = options?.loadingRef ?? saving
  loader.value = true
  try {
    const payload: EcExpressStationSaveRequest = {
      name: form.name.trim(),
      avatarUrl: form.avatarUrl?.trim() || undefined,
      contact: form.contact?.trim() || undefined,
      address: form.address?.trim() || undefined,
      labelPrice: form.labelPrice,
      isDefault: form.isDefault,
      nameAliases: form.nameAliases.map((item) => item.trim()).filter(Boolean),
    }
    if (editingId.value) {
      await updateExpressStation(editingId.value, payload)
      invalidateExpandDetail(editingId.value)
    } else {
      const created = await createExpressStation(payload)
      editingId.value = created.id
    }
    if (!options?.silent) {
      ElMessage.success(t('ecommerce.common.saved'))
    }
    await loadStations()
    if (editingId.value) {
      await loadStationChildren(editingId.value)
      if (expandedRowKeys.value.includes(editingId.value)) {
        await loadExpandDetail(editingId.value)
      }
    }
    return true
  } finally {
    loader.value = false
  }
}

async function onSaveBasicSection() {
  await onSaveStation({ loadingRef: savingBasic })
}

async function ensureStationSavedForSection(): Promise<boolean> {
  if (editingId.value) {
    return true
  }
  return onSaveStation({ silent: true, loadingRef: savingBasic })
}

async function onSavePriceSection() {
  if (!(await ensureStationSavedForSection())) {
    return
  }
  if (priceDialogVisible.value) {
    await onSavePrice({ silent: true, loadingRef: savingPriceSection })
    ElMessage.success(t('ecommerce.express.savePriceSectionSuccess'))
    return
  }
  savingPriceSection.value = true
  try {
    await loadStationChildren(editingId.value!)
    invalidateExpandDetail(editingId.value!)
    if (expandedRowKeys.value.includes(editingId.value!)) {
      await loadExpandDetail(editingId.value!)
    }
    await loadStations()
    ElMessage.success(t('ecommerce.express.savePriceSectionSuccess'))
  } finally {
    savingPriceSection.value = false
  }
}

async function onSaveNoticeSection() {
  if (!(await ensureStationSavedForSection())) {
    return
  }
  if (noticeDialogVisible.value) {
    await onSaveNotice({ silent: true, loadingRef: savingNoticeSection })
    ElMessage.success(t('ecommerce.express.saveNoticeSectionSuccess'))
    return
  }
  savingNoticeSection.value = true
  try {
    await loadStationChildren(editingId.value!)
    invalidateExpandDetail(editingId.value!)
    if (expandedRowKeys.value.includes(editingId.value!)) {
      await loadExpandDetail(editingId.value!)
    }
    await loadStations()
    ElMessage.success(t('ecommerce.express.saveNoticeSectionSuccess'))
  } finally {
    savingNoticeSection.value = false
  }
}

async function onSaveAll() {
  savingAll.value = true
  try {
    const basicOk = await onSaveStation({ silent: true, loadingRef: savingBasic })
    if (!basicOk) {
      return
    }
    if (priceDialogVisible.value) {
      await onSavePrice({ silent: true })
    }
    if (noticeDialogVisible.value) {
      await onSaveNotice({ silent: true })
    }
    ElMessage.success(t('ecommerce.express.saveAllSuccess'))
  } finally {
    savingAll.value = false
  }
}

async function onDelete(row: EcExpressStation) {
  await ElMessageBox.confirm(
    t('ecommerce.express.deleteConfirm', { name: row.name }),
    { type: 'warning' },
  )
  await deleteExpressStation(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  invalidateExpandDetail(row.id)
  expandedRowKeys.value = expandedRowKeys.value.filter((id) => id !== row.id)
  await loadStations()
}

async function onCopy(row: EcExpressStation) {
  await copyExpressStation(row.id)
  ElMessage.success(t('ecommerce.express.copyStationSuccess'))
  await loadStations()
}

function openPriceCreate() {
  priceEditingId.value = null
  resetPriceForm()
  priceCollapseActive.value = ['le1']
  priceDialogVisible.value = true
}

function openPriceEdit(row: EcExpressPrice) {
  priceEditingId.value = row.id
  priceForm.provinceName = row.provinceName
  priceColumns.value.forEach((col) => {
    priceForm[col.key] = row[col.key] ?? null
  })
  priceCollapseActive.value = ['le1', 'mid', 'over3']
  priceDialogVisible.value = true
}

function openPriceCopy(row: EcExpressPrice) {
  priceEditingId.value = null
  resetPriceForm()
  priceColumns.value.forEach((col) => {
    priceForm[col.key] = row[col.key] ?? null
  })
  priceCollapseActive.value = ['le1', 'mid', 'over3']
  priceDialogVisible.value = true
}

async function onSavePrice(options?: { silent?: boolean; loadingRef?: typeof priceSaving }) {
  if (!editingId.value) return false
  if (!priceForm.provinceName.trim()) {
    ElMessage.warning(t('ecommerce.express.provinceRequired'))
    return false
  }

  const loader = options?.loadingRef ?? priceSaving
  loader.value = true
  try {
    const payload = {
      stationId: editingId.value,
      provinceName: priceForm.provinceName.trim(),
      priceW03Kg: priceForm.priceW03Kg,
      priceW05Kg: priceForm.priceW05Kg,
      priceW1Kg: priceForm.priceW1Kg,
      priceW15Kg: priceForm.priceW15Kg,
      priceW2Kg: priceForm.priceW2Kg,
      priceW25Kg: priceForm.priceW25Kg,
      priceW3Kg: priceForm.priceW3Kg,
      over3FirstPrice: priceForm.over3FirstPrice,
      over3AdditionalPrice: priceForm.over3AdditionalPrice,
    }
    if (priceEditingId.value) {
      await updateExpressPrice(priceEditingId.value, payload)
    } else {
      await createExpressPrice(payload)
    }
    if (!options?.silent) {
      ElMessage.success(t('ecommerce.common.saved'))
    }
    priceDialogVisible.value = false
    await loadStationChildren(editingId.value)
    invalidateExpandDetail(editingId.value)
    if (expandedRowKeys.value.includes(editingId.value)) {
      await loadExpandDetail(editingId.value)
    }
    await loadStations()
    return true
  } finally {
    loader.value = false
  }
}

async function onDeletePrice(row: EcExpressPrice) {
  await ElMessageBox.confirm(
    t('ecommerce.express.deletePriceConfirm', { province: row.provinceName }),
    { type: 'warning' },
  )
  await deleteExpressPrice(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  if (editingId.value) {
    await loadStationChildren(editingId.value)
    invalidateExpandDetail(editingId.value)
    if (expandedRowKeys.value.includes(editingId.value)) {
      await loadExpandDetail(editingId.value)
    }
    await loadStations()
  }
}

function openNoticeCreate() {
  noticeEditingId.value = null
  resetNoticeForm()
  noticeDialogVisible.value = true
}

function openNoticeEdit(row: EcExpressNotice) {
  noticeEditingId.value = row.id
  noticeForm.content = row.content
  noticeForm.highlightRed = !!row.highlightRed
  noticeForm.sortOrder = row.sortOrder ?? 0
  noticeDialogVisible.value = true
}

async function onSaveNotice(options?: { silent?: boolean; loadingRef?: typeof noticeSaving }) {
  if (!editingId.value) return false
  if (!noticeForm.content.trim()) {
    ElMessage.warning(t('ecommerce.express.noticeRequired'))
    return false
  }

  const loader = options?.loadingRef ?? noticeSaving
  loader.value = true
  try {
    const payload = {
      stationId: editingId.value,
      content: noticeForm.content.trim(),
      highlightRed: noticeForm.highlightRed,
      sortOrder: noticeForm.sortOrder,
    }
    if (noticeEditingId.value) {
      await updateExpressNotice(noticeEditingId.value, payload)
    } else {
      await createExpressNotice(payload)
    }
    if (!options?.silent) {
      ElMessage.success(t('ecommerce.common.saved'))
    }
    noticeDialogVisible.value = false
    await loadStationChildren(editingId.value)
    invalidateExpandDetail(editingId.value)
    if (expandedRowKeys.value.includes(editingId.value)) {
      await loadExpandDetail(editingId.value)
    }
    await loadStations()
    return true
  } finally {
    loader.value = false
  }
}

async function onDeleteNotice(row: EcExpressNotice) {
  await ElMessageBox.confirm(t('ecommerce.express.deleteNoticeConfirm'), { type: 'warning' })
  await deleteExpressNotice(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  if (editingId.value) {
    await loadStationChildren(editingId.value)
    invalidateExpandDetail(editingId.value)
    if (expandedRowKeys.value.includes(editingId.value)) {
      await loadExpandDetail(editingId.value)
    }
    await loadStations()
  }
}

onMounted(async () => {
  regionOptions.value = await fetchExpressRegions()
  await loadStations()
})

onBeforeUnmount(() => {
  destroyNoticeSortable()
})

defineExpose({ loadStations })
</script>

<style scoped lang="scss">
.panel-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.express-panel__search {
  width: 320px;
}

.express-panel__toolbar-spacer {
  flex: 1;
  min-width: 8px;
}

.express-panel__region-filter {
  width: 240px;
}

.express-panel__default-filter {
  margin-right: 4px;
}

.sub-toolbar {
  margin-bottom: 12px;
}

.tab-hint,
.form-hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  padding: 8px 0;
}

.form-hint {
  padding: 4px 0 0;
  line-height: 1.5;
}

.express-form-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.express-field-tip {
  font-size: 14px;
  color: var(--wr-muted, #999);
  cursor: help;
  vertical-align: middle;

  &:hover {
    color: var(--el-color-primary);
  }
}

.price-table {
  width: 100%;
}

.express-station-table {
  :deep(.express-station-row) {
    cursor: pointer;
  }

  :deep(.express-station-row.is-expanded > td) {
    background: var(--wr-stat-blue-bg, #eff6ff) !important;
  }

  :deep(.table-actions-cell) {
    cursor: default;
  }

  :deep(.el-table__header-wrapper) {
    position: sticky;
    top: 0;
    z-index: 2;
  }
}

.express-station-name {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.express-station-name__text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.express-default-tag {
  flex-shrink: 0;
  --el-tag-bg-color: #c62828;
  --el-tag-border-color: #b71c1c;
  --el-tag-text-color: #fff;
  font-weight: 600;
}

.express-save-btn {
  --el-button-bg-color: #15803d;
  --el-button-border-color: #15803d;
  --el-button-hover-bg-color: #166534;
  --el-button-hover-border-color: #166534;
  --el-button-active-bg-color: #14532d;
  --el-button-active-border-color: #14532d;
  --el-button-text-color: #fff;
  --el-button-hover-text-color: #fff;
  --el-button-active-text-color: #fff;
  --el-button-disabled-bg-color: #86a88f;
  --el-button-disabled-border-color: #86a88f;
}

.express-default-switch {
  --el-switch-on-color: #15803d;
}

.express-edit__price-toolbar {
  margin-bottom: 10px;
}

.express-edit__price-search {
  max-width: 280px;
}

.express-notice-drag-handle {
  cursor: grab;
  color: var(--wr-muted, #999);
  font-size: 16px;
  vertical-align: middle;

  &:active {
    cursor: grabbing;
  }
}

.express-notice-drag-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

:deep(.express-notice-row--ghost) {
  opacity: 0.45;
  background: var(--el-color-primary-light-9, #ecf5ff) !important;
}

.express-expand {
  display: grid;
  grid-template-columns: minmax(220px, 260px) minmax(0, 1fr) minmax(220px, 280px);
  gap: 16px;
  padding: 16px 20px 20px;
  background: var(--wr-bg, #f9f9fa);
  min-height: 160px;
}

.express-expand__section {
  background: var(--wr-card, #fff);
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 8px;
  padding: 12px 14px;
  min-width: 0;
}

.express-expand__identity {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--wr-border, #e8ecef);
}

.express-expand__identity-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--wr-text, #333);
  margin-bottom: 4px;
}

.express-expand__title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--wr-text, #333);
  display: flex;
  align-items: center;
  gap: 8px;
}

.express-expand__info {
  margin: 0;
}

.express-expand__info-row {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px dashed var(--wr-border, #e8ecef);
  font-size: 13px;
  line-height: 1.5;

  &:last-child {
    border-bottom: none;
  }

  dt {
    margin: 0;
    color: var(--wr-muted, #999);
  }

  dd {
    margin: 0;
    color: var(--wr-text, #333);
    word-break: break-word;
  }
}

.express-expand__heatmap-wrap {
  overflow: auto;
  max-height: 360px;
}

.express-heatmap {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;

  th,
  td {
    border: 1px solid var(--wr-border, #e8ecef);
    padding: 6px 8px;
    text-align: right;
    white-space: nowrap;
  }

  th {
    background: var(--wr-stat-gray-bg, #f3f4f6);
    color: var(--wr-text-secondary, #666);
    font-weight: 600;
    position: sticky;
    top: 0;
    z-index: 1;
  }
}

.express-heatmap__province {
  text-align: left !important;
  position: sticky;
  left: 0;
  z-index: 1;
  background: var(--wr-card, #fff);
  font-weight: 500;
}

.express-heatmap thead .express-heatmap__province {
  background: var(--wr-stat-gray-bg, #f3f4f6);
  z-index: 2;
}

.express-heatmap__cell {
  font-variant-numeric: tabular-nums;
}

.express-heatmap__actions {
  text-align: center !important;
  white-space: nowrap;
}

.express-edit-dialog {
  :deep(.el-dialog) {
    display: flex;
    flex-direction: column;
    max-height: 92vh;
    margin-top: 4vh !important;
    margin-bottom: 4vh;
    overflow: hidden;
  }

  :deep(.el-dialog__header),
  :deep(.el-dialog__footer) {
    flex-shrink: 0;
  }

  :deep(.el-dialog__body) {
    flex: 1 1 auto;
    min-height: 0;
    overflow: hidden;
    padding-top: 8px;
  }
}

.express-edit {
  height: 100%;
  min-height: 0;
}

.express-edit__layout {
  display: grid;
  grid-template-columns: minmax(300px, 360px) minmax(0, 1fr);
  gap: 16px;
  height: 100%;
  min-height: 0;
  max-height: 100%;
  overflow: hidden;
}

.express-edit__left {
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.express-edit__right {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.express-edit__card {
  background: var(--wr-bg, #f9f9fa);
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 10px;
  padding: 14px 16px 16px;
  min-width: 0;
}

.express-edit__card--basic {
  display: flex;
  flex-direction: column;
}

.express-edit__card--prices {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.express-edit__card--notices {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  min-height: 0;
}

.express-edit__profile {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding-bottom: 14px;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--wr-border, #e8ecef);
}

.express-edit__profile-meta {
  min-width: 0;
  padding-top: 4px;
}

.express-edit__profile-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--wr-text, #333);
  margin-bottom: 6px;
}

.express-edit__profile-hint {
  margin: 0;
  font-size: 12px;
  color: var(--wr-muted, #999);
  line-height: 1.5;
}

.express-edit__form {
  flex: 1;
  min-height: 0;
}

.express-alias-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  width: 100%;
}

.express-alias-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  padding: 4px 8px 4px 10px;
  border: 1px solid;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.4;
}

.express-alias-tag__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.express-alias-tag__remove {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;

  &:hover {
    color: #dc2626;
  }
}

.express-alias-add {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px dashed #86efac;
  border-radius: 6px;
  background: #fff;
  color: #059669;
  font-size: 12px;
  line-height: 1.4;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;

  .el-icon {
    font-size: 14px;
  }

  &:hover {
    border-color: #22c55e;
    background: #f0fdf4;
  }
}

.express-alias-popover {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.express-alias-popover__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.express-edit__notice-table {
  flex: 1;
  min-height: 0;
  width: 100%;

  :deep(.el-table__body-wrapper) {
    max-height: min(28vh, 280px);
    overflow-y: auto;
  }

  :deep(.express-notice-drag-col .cell) {
    display: flex;
    align-items: center;
    justify-content: center;
    padding-left: 0;
    padding-right: 0;
  }

  :deep(.el-table__cell) {
    border-right: none !important;
    text-align: center;
  }

  :deep(.el-table__header .el-table__cell) {
    border-right: none !important;
  }

  :deep(.el-table__inner-wrapper::before),
  :deep(.el-table__border-left-patch) {
    display: none;
  }
}

.express-edit__card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.express-edit__card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.express-edit__card-title {
  margin: 0;
  padding-left: 10px;
  border-left: 3px solid var(--el-color-primary, #409eff);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--wr-text, #333);
  display: flex;
  align-items: center;
  gap: 8px;
}

.express-edit__heatmap-wrap {
  overflow: auto;
  max-height: min(42vh, 420px);
  background: var(--wr-card, #fff);
  border-radius: 8px;
  border: 1px solid var(--wr-border, #e8ecef);
}

.express-expand__notice-list {
  margin: 0;
  padding-left: 18px;
  line-height: 1.7;
  font-size: 13px;
}

.express-expand__empty {
  grid-column: 1 / -1;
  text-align: center;
  color: var(--wr-muted, #999);
  padding: 24px;
}

.text-red {
  color: var(--el-color-danger);
  font-weight: 500;
}

.express-price-dialog {
  :deep(.el-dialog__body) {
    padding-top: 12px;
  }
}

.express-price-dialog__cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.express-price-dialog__card {
  background: var(--wr-bg, #f9f9fa);
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 10px;
  padding: 14px 16px 16px;
}

.express-price-dialog__section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.express-price-dialog__section-title {
  margin: 0 0 12px;
  padding-left: 10px;
  border-left: 3px solid var(--el-color-primary, #409eff);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--wr-text, #333);

  &--inline {
    margin-bottom: 0;
  }
}

.express-price-dialog__copy-link {
  font-size: 13px;

  .el-icon {
    margin-right: 4px;
  }
}

.express-price-dialog__recent {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.express-price-dialog__recent-label {
  font-size: 12px;
  color: var(--wr-muted, #999);
}

.express-price-dialog__recent-tag {
  padding: 2px 10px;
  border: 1px solid #bbf7d0;
  border-radius: 999px;
  background: #ecfdf5;
  color: #047857;
  font-size: 12px;
  line-height: 1.5;
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease;

  &:hover {
    border-color: #22c55e;
    background: #f0fdf4;
  }
}

.express-price-dialog__collapse {
  border: none;
  background: var(--wr-card, #fff);
  border-radius: 8px;
  padding: 0;

  :deep(.el-collapse-item__header) {
    font-size: 13px;
    font-weight: 600;
    border-bottom: none;
    padding-left: 24px;
    padding-right: 24px;
  }

  :deep(.el-collapse-item) {
    border-bottom: 1px solid var(--wr-border, #e8ecef);

    &:last-child {
      border-bottom: none;
    }
  }

  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
  }

  :deep(.el-collapse-item__content) {
    padding: 0 24px 12px;
  }
}

.express-price-dialog__price-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px 24px;
}

.express-price-dialog__price-field {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 100px;
  gap: 8px;
  align-items: center;
}

.express-price-dialog__price-label {
  text-align: right;
  font-size: 13px;
  color: var(--wr-muted, #999);
  line-height: 1.4;
  white-space: nowrap;
}

.express-price-dialog__price-input {
  width: 100px;
  flex-shrink: 0;
}

.express-notice-dialog {
  :deep(.el-dialog__body) {
    padding-top: 8px;
  }
}

.express-notice-dialog__body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.express-notice-dialog__form {
  :deep(.el-form-item__label) {
    font-weight: 500;
  }
}

.express-notice-dialog__highlight-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.express-notice-highlight-switch {
  --el-switch-on-color: var(--el-color-danger);
}

.express-notice-dialog__highlight-hint {
  font-size: 12px;
  color: var(--wr-muted, #999);
  line-height: 1.5;
}

.express-notice-dialog__sort-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--wr-muted, #999);
  line-height: 1.5;
}

.express-notice-dialog__preview {
  padding: 12px 14px 14px;
  border-radius: 10px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.express-notice-dialog__preview-title {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 600;
  color: #047857;
}

.express-notice-preview-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid var(--wr-border, #e8ecef);
  min-height: 44px;
}

.express-notice-preview-card__drag {
  flex-shrink: 0;
  color: var(--wr-muted, #999);
  font-size: 16px;
}

.express-notice-preview-card__sort {
  flex-shrink: 0;
  min-width: 20px;
  font-size: 13px;
  font-weight: 600;
  color: var(--wr-text, #333);
  text-align: center;

  &.text-red {
    color: var(--el-color-danger);
  }
}

.express-notice-preview-card__content {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--wr-text, #333);
  word-break: break-word;

  &.text-red {
    color: var(--el-color-danger);
    font-weight: 500;
  }
}

@media (max-width: 960px) {
  .express-edit-dialog {
    :deep(.el-dialog) {
      max-height: 96vh;
    }
  }

  .express-edit__layout {
    grid-template-columns: 1fr;
    overflow-y: auto;
  }

  .express-edit__left,
  .express-edit__right {
    overflow: visible;
  }

  .express-edit__card--basic {
    height: auto;
  }

  .express-edit__heatmap-wrap {
    max-height: min(50vh, 360px);
  }

  .express-price-dialog__price-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1200px) {
  .express-expand {
    grid-template-columns: 1fr;
  }
}
</style>
