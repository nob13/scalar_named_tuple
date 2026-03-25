<script setup lang="ts">
import { computed, ref, useSlots, watchEffect, type VNode, type VNodeArrayChildren } from 'vue'
import { codeToHtml } from 'shiki'

const props = withDefaults(defineProps<{
  code?: string
  lang?: string
}>(), {
  lang: 'scala',
})

const slots = useSlots()
const highlighted = ref('')

function flattenText(children: VNodeArrayChildren): string {
  let text = ''

  for (const child of children) {
    if (typeof child === 'string' || typeof child === 'number') {
      text += String(child)
      continue
    }

    if (Array.isArray(child)) {
      text += flattenText(child)
      continue
    }

    if (child && typeof child === 'object') {
      const vnode = child as VNode
      const nested = vnode.children
      if (typeof nested === 'string') {
        text += nested
      }
      else if (Array.isArray(nested)) {
        text += flattenText(nested)
      }
    }
  }

  return text
}

const sourceCode = computed(() => {
  if (props.code)
    return props.code

  const content = slots.default?.() ?? []
  return flattenText(content).trim()
})

watchEffect(async () => {
  if (!sourceCode.value) {
    highlighted.value = ''
    return
  }

  const html = await codeToHtml(sourceCode.value, {
    lang: props.lang,
    theme: 'vitesse-light',
  })

  highlighted.value = html
    .replace(/^<pre[^>]*><code>/, '')
    .replace(/<\/code><\/pre>$/, '')
})
</script>

<template>
  <span class="inline-code" v-html="highlighted"></span>
</template>

<style scoped>
.inline-code :deep(.line) {
  display: inline;
}

.inline-code :deep(span) {
  white-space: pre-wrap;
}

.inline-code {
  display: inline;
  padding: 0.08em 0.35em;
  border-radius: 0.35em;
  background: #f5f3ff;
  white-space: nowrap;
  font-size: 0.95em;
  font-family: Menlo, monospace;
}
</style>
