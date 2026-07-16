import { useLibraryStore } from '@/stores/library'

export function useLibraryTracker() {
  const store = useLibraryStore()

  function trackFileAction(
    event: string,
    fileId: number,
    extra?: Record<string, string | number | boolean>,
  ) {
    store.track({
      event,
      fileId,
      paramsJson: extra ? JSON.stringify(extra) : null,
    })
  }

  function trackFolderAction(
    event: string,
    folderId: number,
    extra?: Record<string, string | number | boolean>,
  ) {
    store.track({
      event,
      folderId,
      paramsJson: extra ? JSON.stringify(extra) : null,
    })
  }

  function trackSearch(keyword: string, resultCount: number) {
    store.track({
      event: 'search_execute',
      paramsJson: JSON.stringify({ keyword, resultCount }),
    })
  }

  function trackViewModeChange(fromMode: string, toMode: string) {
    store.track({
      event: 'view_mode_switch',
      paramsJson: JSON.stringify({ fromMode, toMode }),
    })
  }

  function trackKbAction(fileId: number, marked: boolean) {
    trackFileAction('kb_file_mark', fileId, { marked })
  }

  return {
    trackFileAction,
    trackFolderAction,
    trackSearch,
    trackViewModeChange,
    trackKbAction,
  }
}
