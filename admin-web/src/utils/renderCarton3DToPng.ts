import * as THREE from 'three'
import { RoundedBoxGeometry } from 'three/examples/jsm/geometries/RoundedBoxGeometry.js'
import { resolveCartonMaterial, type CartonMaterialDef } from '@/constants/cartonMaterials'

const RENDER_SIZE = 1024
const PIXEL_RATIO = 2

const TAPE_COLORS: Record<string, number> = {
  'material-kraft': 0x9a6b38,
  'material-white': 0xc8c8c8,
  'material-corrugated': 0x8f6234,
  'material-express': 0x9a6b38,
}

const textureCache = new Map<string, Promise<THREE.Texture>>()

export interface RenderCarton3DOptions {
  lengthCm: number
  widthCm: number
  heightCm: number
  illustrationVariant?: number | null
  seed?: number | string
  canvasWidth?: number
  canvasHeight?: number
  backgroundColor?: string
}

const TEXTURE_RASTER_SIZE = 512

function configureTexture(texture: THREE.Texture) {
  texture.colorSpace = THREE.SRGBColorSpace
  texture.wrapS = THREE.RepeatWrapping
  texture.wrapT = THREE.RepeatWrapping
  texture.anisotropy = 8
  texture.needsUpdate = true
  return texture
}

async function decodeImageSource(url: string): Promise<CanvasImageSource> {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`纹理加载失败 (${response.status}): ${url}`)
  }
  const blob = await response.blob()
  const isSvg = blob.type === 'image/svg+xml' || /\.svg($|\?)/i.test(url)
  const objectUrl = URL.createObjectURL(blob)
  try {
    const image = await new Promise<HTMLImageElement>((resolve, reject) => {
      const img = new Image()
      img.onload = () => resolve(img)
      img.onerror = () => reject(new Error(`纹理解码失败: ${url}`))
      img.src = objectUrl
    })
    if (!isSvg) return image

    const canvas = document.createElement('canvas')
    canvas.width = TEXTURE_RASTER_SIZE
    canvas.height = TEXTURE_RASTER_SIZE
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('Canvas 2D 不可用')
    ctx.drawImage(image, 0, 0, TEXTURE_RASTER_SIZE, TEXTURE_RASTER_SIZE)
    return canvas
  } finally {
    URL.revokeObjectURL(objectUrl)
  }
}

function loadTexture(url: string): Promise<THREE.Texture> {
  if (!textureCache.has(url)) {
    textureCache.set(
      url,
      decodeImageSource(url).then((source) => {
        const texture =
          source instanceof HTMLImageElement ? new THREE.Texture(source) : new THREE.CanvasTexture(source)
        return configureTexture(texture)
      }),
    )
  }
  return textureCache.get(url)!
}

function drawShippingIcons(ctx: CanvasRenderingContext2D, size: number) {
  const iconSize = size * 0.19
  const gap = size * 0.035
  const startX = size * 0.06
  const baseY = size * 0.72

  const icons: Array<(x: number, y: number, s: number) => void> = [
    (x, y, s) => {
      const cx = x + s / 2
      const cy = y + s / 2
      const h = s * 0.22
      ctx.strokeStyle = '#111827'
      ctx.lineWidth = s * 0.045
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'
      ctx.beginPath()
      ctx.moveTo(cx, cy - h)
      ctx.lineTo(cx, cy + h * 0.35)
      ctx.moveTo(cx - h * 0.55, cy - h * 0.12)
      ctx.lineTo(cx, cy - h)
      ctx.lineTo(cx + h * 0.55, cy - h * 0.12)
      ctx.stroke()
    },
    (x, y, s) => {
      const cx = x + s / 2
      const top = y + s * 0.22
      const bottom = y + s * 0.78
      ctx.strokeStyle = '#111827'
      ctx.lineWidth = s * 0.04
      ctx.beginPath()
      ctx.moveTo(cx - s * 0.16, bottom)
      ctx.lineTo(cx - s * 0.08, top)
      ctx.lineTo(cx + s * 0.08, top)
      ctx.lineTo(cx + s * 0.16, bottom)
      ctx.closePath()
      ctx.stroke()
      ctx.beginPath()
      ctx.moveTo(cx - s * 0.2, bottom - s * 0.08)
      ctx.lineTo(cx + s * 0.2, bottom - s * 0.08)
      ctx.stroke()
    },
    (x, y, s) => {
      const cx = x + s / 2
      const cy = y + s * 0.4
      const r = s * 0.18
      ctx.strokeStyle = '#111827'
      ctx.lineWidth = s * 0.04
      ctx.beginPath()
      ctx.arc(cx, cy, r, Math.PI, 0)
      ctx.stroke()
      ctx.beginPath()
      ctx.moveTo(cx - r * 0.55, cy + r * 0.12)
      ctx.lineTo(cx + r * 0.2, cy + r * 0.55)
      ctx.moveTo(cx + r * 0.15, cy + r * 0.18)
      ctx.lineTo(cx + r * 0.55, cy + r * 0.55)
      ctx.stroke()
    },
  ]

  icons.forEach((draw, index) => {
    const x = startX + index * (iconSize + gap)
    const y = baseY
    ctx.fillStyle = 'rgba(255,255,255,0.78)'
    ctx.strokeStyle = 'rgba(17,24,39,0.9)'
    ctx.lineWidth = size * 0.006
    const pad = size * 0.012
    ctx.fillRect(x - pad, y - pad, iconSize + pad * 2, iconSize + pad * 2)
    ctx.strokeRect(x - pad, y - pad, iconSize + pad * 2, iconSize + pad * 2)
    draw(x, y, iconSize)
  })
}

function createShippingLabelTexture(): THREE.CanvasTexture {
  const size = 512
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const ctx = canvas.getContext('2d')
  if (ctx) {
    ctx.clearRect(0, 0, size, size)
    drawShippingIcons(ctx, size)
  }
  const texture = new THREE.CanvasTexture(canvas)
  texture.colorSpace = THREE.SRGBColorSpace
  return texture
}

function createCardboardMaterial(texture: THREE.Texture, repeatScale: number): THREE.MeshStandardMaterial {
  const map = texture.clone()
  map.repeat.set(repeatScale, repeatScale)
  map.needsUpdate = true
  return new THREE.MeshStandardMaterial({
    map,
    roughness: 0.9,
    metalness: 0.02,
    color: 0xffffff,
  })
}

function addPackagingTape(
  group: THREE.Group,
  lengthCm: number,
  widthCm: number,
  heightCm: number,
  material: CartonMaterialDef,
) {
  if (!material.tapeOnTop) return

  const color = TAPE_COLORS[material.className] ?? TAPE_COLORS['material-kraft']
  const tapeMat = new THREE.MeshStandardMaterial({
    color,
    roughness: 0.72,
    metalness: 0.04,
  })

  const topTape = new THREE.Mesh(
    new THREE.BoxGeometry(lengthCm * 0.78, 0.18, widthCm * 0.26),
    tapeMat,
  )
  topTape.position.y = heightCm / 2 + 0.07
  topTape.castShadow = true
  group.add(topTape)

  const frontTape = new THREE.Mesh(
    new THREE.BoxGeometry(lengthCm * 0.78, heightCm * 0.1, 0.14),
    tapeMat,
  )
  frontTape.position.set(0, heightCm / 2 - heightCm * 0.05, widthCm / 2 + 0.07)
  frontTape.castShadow = true
  group.add(frontTape)
}

function addShippingLabels(
  group: THREE.Group,
  lengthCm: number,
  widthCm: number,
  heightCm: number,
) {
  const labelTexture = createShippingLabelTexture()
  const label = new THREE.Mesh(
    new THREE.PlaneGeometry(lengthCm * 0.58, heightCm * 0.2),
    new THREE.MeshBasicMaterial({
      map: labelTexture,
      transparent: true,
      depthWrite: false,
      polygonOffset: true,
      polygonOffsetFactor: -1,
      polygonOffsetUnits: -1,
    }),
  )
  label.position.set(0, -heightCm * 0.18, widthCm / 2 + 0.12)
  group.add(label)
}

function setupLights(scene: THREE.Scene, maxDim: number) {
  scene.add(new THREE.AmbientLight(0xffffff, 0.52))

  const key = new THREE.DirectionalLight(0xfff6ee, 1.2)
  key.position.set(-maxDim * 1.4, maxDim * 2.2, maxDim * 1.6)
  key.castShadow = true
  key.shadow.mapSize.set(2048, 2048)
  key.shadow.camera.near = maxDim * 0.2
  key.shadow.camera.far = maxDim * 10
  const spread = maxDim * 2.8
  key.shadow.camera.left = -spread
  key.shadow.camera.right = spread
  key.shadow.camera.top = spread
  key.shadow.camera.bottom = -spread
  key.shadow.bias = -0.00015
  key.shadow.radius = 6
  scene.add(key)

  const fill = new THREE.DirectionalLight(0xe6eeff, 0.38)
  fill.position.set(maxDim * 1.8, maxDim * 0.6, -maxDim * 0.6)
  scene.add(fill)

  const rim = new THREE.DirectionalLight(0xffffff, 0.22)
  rim.position.set(0, maxDim * 0.5, -maxDim * 2)
  scene.add(rim)
}

function setupGround(scene: THREE.Scene, maxDim: number, groundY: number) {
  const ground = new THREE.Mesh(
    new THREE.PlaneGeometry(maxDim * 6, maxDim * 6),
    new THREE.ShadowMaterial({ opacity: 0.14, color: 0x000000 }),
  )
  ground.rotation.x = -Math.PI / 2
  ground.position.y = groundY
  ground.receiveShadow = true
  scene.add(ground)
}

function fitCamera(
  camera: THREE.PerspectiveCamera,
  target: THREE.Object3D,
  aspect: number,
  padding = 1.42,
) {
  const box = new THREE.Box3().setFromObject(target)
  const fitBox = box.clone()
  const size = box.getSize(new THREE.Vector3())
  const maxDim = Math.max(size.x, size.y, size.z)
  fitBox.min.y -= maxDim * 0.06
  fitBox.expandByScalar(maxDim * 0.05)

  const center = fitBox.getCenter(new THREE.Vector3())
  const fitSize = fitBox.getSize(new THREE.Vector3())
  const fitMax = Math.max(fitSize.x, fitSize.y, fitSize.z)

  const fovRad = (camera.fov * Math.PI) / 180
  const fitHeightDistance = fitMax / (2 * Math.tan(fovRad / 2))
  const fitWidthDistance = fitHeightDistance / aspect
  const distance = padding * Math.max(fitHeightDistance, fitWidthDistance)

  const viewDirection = new THREE.Vector3(0.82, 0.58, 0.98).normalize()
  camera.position.copy(center).add(viewDirection.multiplyScalar(distance))
  camera.lookAt(center)
  camera.near = Math.max(distance / 200, 0.1)
  camera.far = distance * 50
  camera.updateProjectionMatrix()
}

function disposeObject3D(root: THREE.Object3D) {
  root.traverse((node) => {
    if (!(node instanceof THREE.Mesh)) return
    node.geometry.dispose()
    const materials = Array.isArray(node.material) ? node.material : [node.material]
    materials.forEach((material) => {
      if (material.map && material.map instanceof THREE.CanvasTexture) {
        material.map.dispose()
      }
      material.dispose()
    })
  })
}

function exportRendererToBlob(renderer: THREE.WebGLRenderer): Promise<Blob> {
  return new Promise((resolve, reject) => {
    renderer.domElement.toBlob(
      (blob) => {
        if (blob) resolve(blob)
        else reject(new Error('Failed to export carton preview PNG'))
      },
      'image/png',
      0.96,
    )
  })
}

export async function renderCarton3DToPngBlob(options: RenderCarton3DOptions): Promise<Blob | null> {
  const lengthCm = Number(options.lengthCm)
  const widthCm = Number(options.widthCm)
  const heightCm = Number(options.heightCm)
  if (lengthCm <= 0 || widthCm <= 0 || heightCm <= 0) return null

  const materialDef = resolveCartonMaterial(options.illustrationVariant, options.seed)
  const cardboardTexture = await loadTexture(materialDef.textureUrl)
  const maxDim = Math.max(lengthCm, widthCm, heightCm)
  const repeatScale = Math.max(maxDim / 22, 1)

  const scene = new THREE.Scene()
  const bgColor = new THREE.Color(options.backgroundColor ?? '#ffffff')
  scene.background = bgColor

  const group = new THREE.Group()

  const boxMaterial = createCardboardMaterial(cardboardTexture, repeatScale)
  const cornerRadius = Math.min(lengthCm, widthCm, heightCm) * 0.018
  const box = new THREE.Mesh(
    new RoundedBoxGeometry(lengthCm, heightCm, widthCm, 5, Math.max(cornerRadius, 0.15)),
    boxMaterial,
  )
  box.castShadow = true
  box.receiveShadow = true
  group.add(box)

  addPackagingTape(group, lengthCm, widthCm, heightCm, materialDef)
  addShippingLabels(group, lengthCm, widthCm, heightCm)
  scene.add(group)

  setupLights(scene, maxDim)
  setupGround(scene, maxDim, -heightCm / 2 - 0.02)

  const renderSize = options.canvasWidth ?? RENDER_SIZE
  const aspect = renderSize / (options.canvasHeight ?? renderSize)
  const camera = new THREE.PerspectiveCamera(30, aspect, 0.1, maxDim * 20)
  fitCamera(camera, group, aspect)

  const renderer = new THREE.WebGLRenderer({
    antialias: true,
    alpha: false,
    preserveDrawingBuffer: true,
  })
  renderer.setPixelRatio(PIXEL_RATIO)
  renderer.setSize(renderSize, options.canvasHeight ?? renderSize)
  renderer.outputColorSpace = THREE.SRGBColorSpace
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.05

  try {
    renderer.render(scene, camera)
    return await exportRendererToBlob(renderer)
  } finally {
    disposeObject3D(scene)
    renderer.dispose()
  }
}
