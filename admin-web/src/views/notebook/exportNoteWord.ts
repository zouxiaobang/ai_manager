export function exportNoteAsWord(title: string, html: string) {
  const safeTitle = (title || '无标题').replace(/[\\/:*?"<>|]/g, '_')
  const body = html?.trim() || '<p></p>'
  const doc = `<!DOCTYPE html>
<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word">
<head><meta charset="utf-8"><title>${safeTitle}</title></head>
<body>${body}</body>
</html>`
  const blob = new Blob(['\ufeff', doc], { type: 'application/msword' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${safeTitle}.doc`
  link.click()
  URL.revokeObjectURL(url)
}
