import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'

const VLC_ENABLED_API = '/api/stream/vlc-enabled'
const FULLY_LOCAL_API = '/api/runtime-config/fully-local'

const AppConfigContext = createContext({
  vlcEnabled: false,
  fullyLocal: false,
  setFullyLocal: () => {},
})

export function AppConfigProvider({ children }) {
  const [vlcEnabled, setVlcEnabled] = useState(false)
  const [fullyLocal, setFullyLocalState] = useState(false)
  const hasFetchedConfig = useRef(false)

  useEffect(() => {
    if (hasFetchedConfig.current) return
    hasFetchedConfig.current = true

    const fetchVlcEnabled = async () => {
      try {
        const response = await fetch(VLC_ENABLED_API)
        if (!response.ok) return

        const text = await response.text()
        setVlcEnabled(text.trim().toLowerCase() === 'true')
      } catch {
        setVlcEnabled(false)
      }
    }

    const fetchFullyLocal = async () => {
      try {
        const response = await fetch(FULLY_LOCAL_API)
        if (!response.ok) return

        const data = await response.json()
        setFullyLocalState(!!data.fullyLocal)
      } catch {
        setFullyLocalState(false)
      }
    }

    fetchVlcEnabled()
    fetchFullyLocal()
  }, [])

  const setFullyLocal = useCallback(async (value) => {
    try {
      const response = await fetch(FULLY_LOCAL_API, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ fullyLocal: value }),
      })
      if (!response.ok) return
      const data = await response.json()
      setFullyLocalState(!!data.fullyLocal)
    } catch {
      // ignore errors; state stays unchanged
    }
  }, [])

  const value = useMemo(() => ({ vlcEnabled, fullyLocal, setFullyLocal }), [vlcEnabled, fullyLocal, setFullyLocal])

  return <AppConfigContext.Provider value={value}>{children}</AppConfigContext.Provider>
}

export function useAppConfig() {
  return useContext(AppConfigContext)
}

