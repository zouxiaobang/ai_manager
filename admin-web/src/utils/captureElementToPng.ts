import html2canvas from 'html2canvas'

const CAPTURE_BACKGROUND = '#e8edf3'

export async function captureElementToPngBlob(
  element: HTMLElement,
  options?: { backgroundColor?: string; scale?: number },
): Promise<Blob | null> {
  const canvas = await html2canvas(element, {
    backgroundColor: options?.backgroundColor ?? CAPTURE_BACKGROUND,
    scale: options?.scale ?? 2,
    useCORS: true,
    logging: false,
  })
  return new Promise((resolve) => {
    canvas.toBlob((blob) => resolve(blob), 'image/png', 0.92)
  })
}
