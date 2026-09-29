import { useEffect, useRef, useState } from 'react'

/** Largura atual de um elemento, pra gráficos SVG desenharem em pixels reais (sem esticar). */
export function useLargura<T extends HTMLElement>() {
  const ref = useRef<T>(null)
  const [largura, setLargura] = useState(0)

  useEffect(() => {
    const elemento = ref.current
    if (!elemento) {
      return
    }
    const observador = new ResizeObserver(([entrada]) => {
      setLargura(Math.floor(entrada.contentRect.width))
    })
    observador.observe(elemento)
    return () => observador.disconnect()
  }, [])

  return [ref, largura] as const
}
