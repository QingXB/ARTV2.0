<template>
  <div class="app-shell">
    <router-view />
  </div>
</template>

<style scoped>
:global(html, body) {
  height: 100%;
  margin: 0;
}

:global(#app) {
  height: 100%;
}

.app-shell {
  width: 100%;
  min-height: 100vh;
  min-height: 100dvh;
}
</style>