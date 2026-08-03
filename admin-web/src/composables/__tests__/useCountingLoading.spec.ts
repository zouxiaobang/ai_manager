import { ref } from 'vue'
import { describe, expect, it } from 'vitest'
import { useCountingLoading } from '../useCountingLoading'

function setup() {
  const active = ref(false)
  const api = useCountingLoading(active)
  return { active, ...api }
}

describe('useCountingLoading 引用计数 loading', () => {
  it('begin 点亮 loading', () => {
    const { active, begin } = setup()
    begin()
    expect(active.value).toBe(true)
  })

  it('多次 begin 后部分 end 不熄灭，全部 end 才熄灭', () => {
    const { active, begin, end } = setup()
    begin()
    begin()
    end()
    expect(active.value).toBe(true)
    end()
    expect(active.value).toBe(false)
  })

  it('end 多于 begin 不出现负计数', () => {
    const { active, end } = setup()
    end()
    end()
    expect(active.value).toBe(false)
  })

  it('silent begin/end 不参与计数', () => {
    const { active, begin, end } = setup()
    begin(true)
    expect(active.value).toBe(false)
    begin()
    end(true)
    expect(active.value).toBe(true)
    end()
    expect(active.value).toBe(false)
  })

  it('reset 清空计数与 loading', () => {
    const { active, begin, reset, end } = setup()
    begin()
    begin()
    reset()
    expect(active.value).toBe(false)
    // reset 后 end 不再熄灭
    end()
    expect(active.value).toBe(false)
  })
})
