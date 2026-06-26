/** 作战屏图标根目录（对应 admin-web/public/icons/） */
export const WAR_ROOM_ICONS_BASE = '/icons'

export type WarRoomIconGroup = 'nav' | 'stats' | 'modules'

/** 获取图标 URL，替换图标时只需覆盖 public/icons 下同名文件 */
export function warRoomIconUrl(group: WarRoomIconGroup, name: string) {
  return `${WAR_ROOM_ICONS_BASE}/${group}/${name}.svg`
}
