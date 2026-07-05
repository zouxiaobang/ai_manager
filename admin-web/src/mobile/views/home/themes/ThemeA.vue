<template>
  <div class="mh-scheme-a">
    <SchemeAHeaderStack />
    <SchemeAOverview />
    <SchemeATodos />
    <SchemeAModules />
  </div>
</template>

<script setup lang="ts">
import SchemeAHeaderStack from './scheme-a/SchemeAHeaderStack.vue'
import SchemeAOverview from './scheme-a/SchemeAOverview.vue'
import SchemeATodos from './scheme-a/SchemeATodos.vue'
import SchemeAModules from './scheme-a/SchemeAModules.vue'
</script>

<style lang="scss">
@use './scheme-a/scheme-a.scss';
</style>
