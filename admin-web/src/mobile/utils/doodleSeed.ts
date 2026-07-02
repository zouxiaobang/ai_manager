/** 将模块 key 转为手绘边框 seed */
export function doodleSeedFromKey(key: string): number {
  let hash = 0
  for (let i = 0; i < key.length; i += 1) {
    hash = (hash * 31 + key.charCodeAt(i)) | 0
  }
  return Math.abs(hash)
}
