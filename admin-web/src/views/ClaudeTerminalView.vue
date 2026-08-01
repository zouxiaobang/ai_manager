<template>
  <div class="claude-terminal-page">
    <ClaudeTerminal ref="terminalRef" />
  </div>
</template>

<script setup lang="ts">
import ClaudeTerminal from '@/components/claude/ClaudeTerminal.vue'
</script>

<style scoped>
.claude-terminal-page {
  width: 100%;
  height: calc(100vh - 60px); /* Subtract header height */
  background: #1a1b1e;
}
</style>
