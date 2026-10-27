import { TAG } from './constants'

export function showBulletin(
  text: string,
  options?: {
    subtitle?: string
    icon?: inu.UIIcon
    duration?: 'short' | 'long' | number
    position?: 'top' | 'bottom'
  }
): void {
  try {
    inu.ui.bulletin({
      text,
      subtitle: options?.subtitle,
      icon: options?.icon ?? inu.icons.common('info'),
      duration: options?.duration ?? 'short',
      position: options?.position,
    }).catch(() => {})
  } catch (e) {
    console.error(TAG, 'Failed to show bulletin, falling back to toast:', e)
    try {
      inu.ui.toast(text)
    } catch {
      // ignore
    }
  }
}
