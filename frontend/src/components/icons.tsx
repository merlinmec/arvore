import type { SVGProps } from 'react'

type IconProps = SVGProps<SVGSVGElement>

const base = {
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.75,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
}

export function IconTree(props: IconProps) {
  return (
    <svg {...base} {...props}>
      <path d="M12 3c-2.4 1.7-4 3.9-4 6.2 0 1 .3 1.9.8 2.6C7.4 12.4 6 14 6 15.9 6 18.2 8 20 10.5 20c.6 0 1.2-.1 1.7-.3" />
      <path d="M12 3c2.4 1.7 4 3.9 4 6.2 0 1-.3 1.9-.8 2.6.9.6 2.3 2.2 2.3 4.1 0 2.3-2 4.1-4.5 4.1-.6 0-1.2-.1-1.7-.3" />
      <path d="M12 12v9" />
    </svg>
  )
}

export function IconCheck(props: IconProps) {
  return (
    <svg {...base} {...props}>
      <path d="M4.5 12.5l5 5 10-11" />
    </svg>
  )
}

export function IconPlus(props: IconProps) {
  return (
    <svg {...base} {...props}>
      <path d="M12 5v14M5 12h14" />
    </svg>
  )
}

export function IconTrash(props: IconProps) {
  return (
    <svg {...base} {...props}>
      <path d="M4 7h16" />
      <path d="M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
      <path d="M6 7l1 13a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1l1-13" />
      <path d="M10 11v6M14 11v6" />
    </svg>
  )
}

export function IconPencil(props: IconProps) {
  return (
    <svg {...base} {...props}>
      <path d="M4 20l.9-3.9L16.6 4.4a1.5 1.5 0 0 1 2.1 0l.9.9a1.5 1.5 0 0 1 0 2.1L7.9 19.1 4 20z" />
      <path d="M14.5 6.5l3 3" />
    </svg>
  )
}

export function IconX(props: IconProps) {
  return (
    <svg {...base} {...props}>
      <path d="M6 6l12 12M18 6L6 18" />
    </svg>
  )
}

export function IconChevronLeft(props: IconProps) {
  return (
    <svg {...base} {...props}>
      <path d="M15 5l-7 7 7 7" />
    </svg>
  )
}

export function IconChevronDown(props: IconProps) {
  return (
    <svg {...base} {...props}>
      <path d="M6 9l6 6 6-6" />
    </svg>
  )
}

export function IconLogOut(props: IconProps) {
  return (
    <svg {...base} {...props}>
      <path d="M9 5H6a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h3" />
      <path d="M14 8l4 4-4 4" />
      <path d="M18 12H9" />
    </svg>
  )
}

export function IconFlame(props: IconProps) {
  return (
    <svg {...base} {...props}>
      <path d="M12 2.5c.6 2 .2 3.4-.8 4.6-1 1.2-2.3 2-2.3 4.1 0 2.6 2 4.8 3.9 4.8-.7-1-1-1.9-.7-2.9.3-1 1-1.5 1.2-2.6.8 1 1.7 2.2 1.7 3.9 0 2.6-2.1 4.6-4.7 4.6-2.9 0-5.3-2.3-5.3-5.6 0-4.2 3.2-5.8 4.4-8.3.4-.9.5-1.8.6-2.6z" />
    </svg>
  )
}

export function IconInbox(props: IconProps) {
  return (
    <svg {...base} {...props}>
      <path d="M4 12h4l1.8 3h4.4l1.8-3h4" />
      <path d="M6 12L4.3 5.6A1 1 0 0 1 5.3 4h13.4a1 1 0 0 1 1 1.6L18 12" />
      <path d="M4 12v6a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-6" />
    </svg>
  )
}
