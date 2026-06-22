<template>
  <svg
    class="mobile-todo-header-icon"
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <!-- 日历 -->
    <rect x="3" y="5" width="14" height="14" rx="1.5" stroke="currentColor" stroke-width="1.5" />
    <path d="M3 9.5h14" stroke="currentColor" stroke-width="1.5" />
    <path d="M6.5 3v3M13.5 3v3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
    <circle cx="6.5" cy="12" r="0.75" fill="currentColor" />
    <circle cx="9.5" cy="12" r="0.75" fill="currentColor" />
    <circle cx="12.5" cy="12" r="0.75" fill="currentColor" />
    <circle cx="6.5" cy="15" r="0.75" fill="currentColor" />
    <circle cx="9.5" cy="15" r="0.75" fill="currentColor" />
    <circle cx="12.5" cy="15" r="0.75" fill="currentColor" />
    <!-- 右下角时钟 -->
    <circle cx="17.5" cy="17" r="5" stroke="currentColor" stroke-width="1.5" />
    <path d="M17.5 17V14.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
    <path d="M17.5 17h2.2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
  </svg>
</template>

<style scoped>
.mobile-todo-header-icon {
  display: block;
  width: 26px;
  height: 26px;
}
</style>
