<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { createEditor, createToolbar } from '@wangeditor/editor'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import '@wangeditor/editor/dist/css/style.css'
import request from '@/utils/request'
import { notifyWarning } from '@/utils/feedback'

interface ImageUploadResult {
  url: string
  fileName?: string
}

interface Props {
  modelValue?: string
  disabled?: boolean
  ticketId?: number
  placeholder?: string
  /** 编辑区最小高度（px）；固定高度模式下即为编辑区高度 */
  height?: number
  /** 为 true 时编辑区随内容增高，直至 maxHeight */
  autoGrow?: boolean
  maxHeight?: number
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  disabled: false,
  ticketId: undefined,
  placeholder: '请输入内容，支持粘贴图片...',
  height: 220,
  autoGrow: false,
  maxHeight: 520,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const toolbarContainer = ref<HTMLElement>()
const editorContainer = ref<HTMLElement>()
let editorInstance: IDomEditor | null = null

function applyAutoGrowLayout() {
  if (!props.autoGrow || !editorContainer.value) {
    return
  }
  const root = editorContainer.value
  const textContainer = root.querySelector('.w-e-text-container') as HTMLElement | null
  const scrollEl = root.querySelector('.w-e-scroll') as HTMLElement | null
  const editable = root.querySelector('[data-slate-editor]') as HTMLElement | null
  if (!textContainer || !scrollEl || !editable) {
    return
  }
  scrollEl.style.height = 'auto'
  scrollEl.style.overflow = 'visible'
  textContainer.style.height = 'auto'
  const contentH = editable.scrollHeight
  const next = Math.min(Math.max(contentH, props.height), props.maxHeight)
  root.style.height = `${next}px`
  if (contentH > props.maxHeight) {
    scrollEl.style.overflow = 'auto'
    scrollEl.style.height = '100%'
  }
}

function scheduleAutoGrowLayout() {
  if (!props.autoGrow) {
    return
  }
  void nextTick(() => {
    requestAnimationFrame(() => applyAutoGrowLayout())
  })
}

function buildEditorConfig(): Partial<IEditorConfig> {
  return {
    placeholder: props.placeholder,
    readOnly: props.disabled,
    scroll: !props.autoGrow,
    MENU_CONF: {
      uploadImage: {
        async customUpload(
          file: File,
          insertFn: (url: string, alt: string, href: string) => void,
        ) {
          if (!props.ticketId) {
            notifyWarning('当前无法上传图片，请确认已打开有效工单页面')
            return
          }
          const editor = editorInstance
          if (!editor) {
            return
          }
          try {
            editor.showProgressBar(1)
            const formData = new FormData()
            formData.append('file', file)
            const result = await request.post<ImageUploadResult>(
              `/ticket/${props.ticketId}/image/upload`,
              formData,
              {
                onUploadProgress: (evt) => {
                  if (!evt.total) {
                    return
                  }
                  const pct = Math.min(99, Math.round((evt.loaded / evt.total) * 100))
                  editor.showProgressBar(Math.max(1, pct))
                },
              },
            )
            editor.showProgressBar(100)
            insertFn(result.url, result.fileName || 'image', '')
          } catch {
            editor.showProgressBar(100)
          }
        },
      },
    },
    onChange(editor: IDomEditor) {
      const html = editor.getHtml()
      const isEmpty = html === '<p><br></p>'
      emit('update:modelValue', isEmpty ? '' : html)
      scheduleAutoGrowLayout()
    },
  }
}

function buildToolbarConfig(): Partial<IToolbarConfig> {
  return {
    excludeKeys: ['fullScreen', 'group-video'],
  }
}

function initEditor() {
  if (!editorContainer.value || !toolbarContainer.value) return

  const editorConfig = buildEditorConfig()

  editorInstance = createEditor({
    selector: editorContainer.value,
    html: props.modelValue || '',
    config: editorConfig,
    mode: 'default',
  })

  createToolbar({
    editor: editorInstance,
    selector: toolbarContainer.value,
    config: buildToolbarConfig(),
    mode: 'default',
  })

  if (props.disabled) {
    editorInstance.disable()
  }
  scheduleAutoGrowLayout()
}

watch(
  () => props.modelValue,
  (newVal) => {
    if (!editorInstance) return
    const currentHtml = editorInstance.getHtml()
    const normalizedCurrent = currentHtml === '<p><br></p>' ? '' : currentHtml
    const normalizedNew = newVal || ''
    if (normalizedCurrent !== normalizedNew) {
      editorInstance.setHtml(normalizedNew)
      scheduleAutoGrowLayout()
    }
  },
)

watch(
  () => props.disabled,
  (disabled) => {
    if (!editorInstance) return
    if (disabled) {
      editorInstance.disable()
    } else {
      editorInstance.enable()
    }
  },
)

watch(
  () => [props.height, props.maxHeight, props.autoGrow] as const,
  () => {
    scheduleAutoGrowLayout()
  },
)

onMounted(() => {
  initEditor()
})

onBeforeUnmount(() => {
  editorInstance?.destroy()
  editorInstance = null
})
</script>

<template>
  <div class="rich-text-editor" :class="{ 'is-disabled': disabled, 'is-autogrow': autoGrow }">
    <div ref="toolbarContainer" class="editor-toolbar" />
    <div
      ref="editorContainer"
      class="editor-content"
      :class="{ 'editor-content-autogrow': autoGrow }"
      :style="
        autoGrow
          ? {
              minHeight: `${height}px`,
              maxHeight: `${maxHeight}px`,
            }
          : { height: `${height}px` }
      "
    />
  </div>
</template>

<style lang="scss" scoped>
.rich-text-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  width: 100%;
  background: #fff;

  &.is-disabled {
    opacity: 0.7;
    cursor: not-allowed;
    background: #f5f7fa;
  }

  .editor-toolbar {
    border-bottom: 1px solid #dcdfe6;
    background: #fafafa;
  }

  .editor-content {
    background: #fff;
    overflow-y: auto;
  }

  &.is-autogrow .editor-content-autogrow {
    overflow-y: hidden;
  }

  /* 避免 min-height:100% 在自适应高度下与父级循环依赖，导致空白时高度异常 */
  &.is-autogrow :deep(.w-e-text-container [data-slate-editor]) {
    min-height: 0;
  }
}
</style>
