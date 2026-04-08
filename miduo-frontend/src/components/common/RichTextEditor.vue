<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, useTemplateRef, watch } from 'vue'
import { createEditor, createToolbar } from '@wangeditor/editor'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import { Editor as SlateEditor, Element as SlateElement, Range as SlateRange } from 'slate'
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
  /** 为 true 时在正文内输入 @ 可触发选人浮层（由父组件渲染） */
  mentionTrigger?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  disabled: false,
  ticketId: undefined,
  placeholder: '请输入内容，支持粘贴图片...',
  height: 220,
  autoGrow: false,
  maxHeight: 520,
  mentionTrigger: false,
})

export interface MentionPanelAnchor {
  top: number
  left: number
  width: number
  height: number
}

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'mention-panel': [
    payload:
      | { open: false }
      | { open: true; keyword: string; anchor: MentionPanelAnchor },
  ]
}>()

const toolbarContainer = ref<HTMLElement>()
const editorContainer = useTemplateRef<HTMLElement>('editorContainer')
let editorInstance: IDomEditor | null = null
let unbindMentionListener: (() => void) | null = null

function bindMentionChangeListener() {
  if (!editorInstance || unbindMentionListener) {
    return
  }
  const onEditorChange = () => {
    void nextTick(() => syncMentionPanelFromEditor())
  }
  editorInstance.on('change', onEditorChange)
  unbindMentionListener = () => {
    editorInstance?.off('change', onEditorChange)
    unbindMentionListener = null
  }
}

function getTextInBlockBeforeCursor(editor: IDomEditor): string {
  const { selection } = editor
  if (!selection || !SlateRange.isCollapsed(selection)) {
    return ''
  }
  const [cursor] = SlateRange.edges(selection)
  const blockAbove = SlateEditor.above(editor, {
    at: cursor,
    match: (n) => SlateElement.isElement(n) && SlateEditor.isBlock(editor, n),
  })
  if (!blockAbove) {
    return ''
  }
  const [, path] = blockAbove
  const start = SlateEditor.start(editor, path)
  return SlateEditor.string(editor, { anchor: start, focus: cursor })
}

function getSelectionAnchorRect(): MentionPanelAnchor | null {
  try {
    const domSel = window.getSelection()
    if (!domSel || domSel.rangeCount === 0) {
      return null
    }
    const r = domSel.getRangeAt(0).cloneRange()
    const rects = r.getClientRects()
    const box = rects.length > 0 ? rects[rects.length - 1] : r.getBoundingClientRect()
    if (!box || (box.width === 0 && box.height === 0)) {
      return null
    }
    return { top: box.top, left: box.left, width: box.width, height: box.height }
  } catch {
    return null
  }
}

function syncMentionPanelFromEditor() {
  if (!props.mentionTrigger || !editorInstance || editorInstance.isDisabled()) {
    emit('mention-panel', { open: false })
    return
  }
  const editor = editorInstance
  const before = getTextInBlockBeforeCursor(editor)
  const at = before.lastIndexOf('@')
  if (at < 0) {
    emit('mention-panel', { open: false })
    return
  }
  const afterAt = before.slice(at + 1)
  if (afterAt.includes('\n')) {
    emit('mention-panel', { open: false })
    return
  }
  if (afterAt.startsWith('@')) {
    emit('mention-panel', { open: false })
    return
  }
  if (afterAt.length > 40) {
    emit('mention-panel', { open: false })
    return
  }
  const anchor = getSelectionAnchorRect()
  if (!anchor) {
    emit('mention-panel', { open: false })
    return
  }
  emit('mention-panel', { open: true, keyword: afterAt, anchor })
}

/** 在光标处插入 HTML（用于 @ 提及等）；依赖 wangEditor 内部 API */
function insertHtml(html: string) {
  if (!editorInstance || !html) {
    return
  }
  editorInstance.focus()
  const ed = editorInstance as IDomEditor & { dangerouslyInsertHtml?: (h: string) => void }
  if (typeof ed.dangerouslyInsertHtml === 'function') {
    ed.dangerouslyInsertHtml(html)
  }
}

/** 删除光标前的若干「字符」（用于去掉编辑器内输入的 @关键词） */
function deleteBackwardChars(count: number) {
  if (!editorInstance || count <= 0) {
    return
  }
  editorInstance.focus()
  for (let i = 0; i < count; i++) {
    editorInstance.deleteBackward('character')
  }
}

defineExpose({
  insertHtml,
  deleteBackwardChars,
})

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
  if (props.mentionTrigger) {
    bindMentionChangeListener()
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

watch(
  () => props.mentionTrigger,
  (enabled) => {
    if (!editorInstance) {
      return
    }
    if (enabled) {
      bindMentionChangeListener()
    } else {
      unbindMentionListener?.()
      emit('mention-panel', { open: false })
    }
  },
)

onMounted(() => {
  initEditor()
})

onBeforeUnmount(() => {
  unbindMentionListener?.()
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
