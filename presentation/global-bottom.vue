<script setup>
import { computed } from 'vue'
import { useNav, useSlideContext } from '@slidev/client'

const { $page } = useSlideContext()
const { currentSlideRoute } = useNav()

const footerText = computed(() => {
  const subtitle = String(currentSlideRoute.value?.meta?.slide?.frontmatter?.subtitle ?? '').trim()
  return subtitle ? `${subtitle} / ${$page.value}` : `${$page.value}`
})
</script>

<template>
  <div
    v-if="$page > 1"
    class="absolute bottom-3 right-4 text-sm px-2 py-1 rounded"
  >
    {{ footerText }}
  </div>
</template>
