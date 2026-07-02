export type MobileHomeThemeId = 'scheme-a' | 'scheme-b' | 'scheme-c' | 'scheme-d'

export interface MobileHomeThemeOption {
  id: MobileHomeThemeId
  labelKey: string
  descKey: string
}

export const MOBILE_HOME_THEME_DEFAULT: MobileHomeThemeId = 'scheme-a'

export const mobileHomeThemeOptions: MobileHomeThemeOption[] = [
  {
    id: 'scheme-a',
    labelKey: 'mobile.settings.homeThemeSchemeA',
    descKey: 'mobile.settings.homeThemeSchemeADesc',
  },
  {
    id: 'scheme-b',
    labelKey: 'mobile.settings.homeThemeSchemeB',
    descKey: 'mobile.settings.homeThemeSchemeBDesc',
  },
  {
    id: 'scheme-c',
    labelKey: 'mobile.settings.homeThemeSchemeC',
    descKey: 'mobile.settings.homeThemeSchemeCDesc',
  },
  {
    id: 'scheme-d',
    labelKey: 'mobile.settings.homeThemeSchemeD',
    descKey: 'mobile.settings.homeThemeSchemeDDesc',
  },
]

export function isMobileHomeThemeId(value: string | null | undefined): value is MobileHomeThemeId {
  return value === 'scheme-a' || value === 'scheme-b' || value === 'scheme-c' || value === 'scheme-d'
}
