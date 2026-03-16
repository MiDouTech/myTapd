<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { createEditor, createToolbar } from '@wangeditor/editor'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import '@wangeditor/editor/dist/css/style.css'
import request from '@/utils/request'

interface ImageUploadResult {
  url: string
  fileName?: string
}

interface Props {
  modelValue?: string
  disabled?: boolean
  ticketId?: number
  placeholder?: string
  height?: number
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  disabled: false,
  ticketId: undefined,
  placeholder: '请输入内容，支持粘贴图片...',
  height: 220,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const toolbarContainer = ref<HTMLElement>()
const editorContainer = ref<HTMLElement>()
let editorInstance: IDomEditor | null = null

function buildEditorConfig(): Partial<IEditorConfig> {
  return {
    placeholder: props.placeholder,
    readOnly: props.disabled,
    MENU_CONF: {
      uploadImage: {
        async customUpload(
          file: File,
          insertFn: (url: string, alt: string, href: string) => void,
        ) {
          if (!props.ticketId) {
            return
          }
          try {
            const formData = new FormData()
            formData.append('file', file)
            const result = await request.post<ImageUploadResult>(
              `/ticket/${props.ticketId}/image/upload`,
              formData,
            )
            insertFn(result.url, result.fileName || 'image', '')
          } catch {
            // 图片上传失败，不插入内容
          }
        },
      },
    },
    onChange(editor: IDomEditor) {
      const html = editor.getHtml()
      const isEmpty = html === '<p><br></p>'
      emit('update:modelValue', isEmpty ? '' : html)
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

onMounted(() => {
  initEditor()
})

onBeforeUnmount(() => {
  editorInstance?.destroy()
  editorInstance = null
})
</script>

<template>
  <div class="rich-text-editor" :class="{ 'is-disabled': disabled }">
    <div ref="toolbarContainer" class="editor-toolbar" />
    <div ref="editorContainer" class="editor-content" :style="{ height: `${height}px` }" />
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
}
</style>
