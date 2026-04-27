import { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react'

const VLC_ENABLED_API = '/api/stream/vlc-enabled'

const AppConfigContext = createContext({
  vlcEnabled: false,
})

export function AppConfigProvider({ children }) {
  const [vlcEnabled, setVlcEnabled] = useState(false)
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

    fetchVlcEnabled()
  }, [])

  const value = useMemo(() => ({ vlcEnabled }), [vlcEnabled])

  return <AppConfigContext.Provider value={value}>{children}</AppConfigContext.Provider>
}

export function useAppConfig() {
  return useContext(AppConfigContext)
}

