import { useEffect, useRef, useState } from 'react';
import type { PlaceDto } from '../api/placeApi';

type KakaoLatLng = object;
type KakaoMap = {
  setCenter: (position: KakaoLatLng) => void;
  relayout?: () => void;
};
type KakaoMarker = {
  setMap: (map: KakaoMap | null) => void;
};
type KakaoInfoWindow = {
  open: (map: KakaoMap, marker: KakaoMarker) => void;
  close: () => void;
};
type KakaoMaps = {
  load: (callback: () => void) => void;
  LatLng: new (lat: number, lng: number) => KakaoLatLng;
  Map: new (container: HTMLElement, options: { center: KakaoLatLng; level: number }) => KakaoMap;
  Marker: new (options: { map: KakaoMap; position: KakaoLatLng }) => KakaoMarker;
  InfoWindow: new (options: { content: HTMLElement }) => KakaoInfoWindow;
  event: { addListener: (target: KakaoMarker, event: 'click', callback: () => void) => void };
};
type KakaoSdk = { maps: KakaoMaps };

declare global {
  interface Window {
    kakao?: KakaoSdk;
  }
}

let sdkPromise: Promise<KakaoSdk> | null = null;

function loadKakaoSdk(key: string) {
  if (window.kakao) return Promise.resolve(window.kakao);
  if (sdkPromise) return sdkPromise;

  sdkPromise = new Promise<KakaoSdk>((resolve, reject) => {
    const script = document.createElement('script');
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(key)}&autoload=false`;
    script.async = true;
    script.onload = () => {
      const kakao = window.kakao;
      if (!kakao) {
        sdkPromise = null;
        reject(new Error('Kakao Maps SDK did not initialize'));
        return;
      }
      kakao.maps.load(() => resolve(kakao));
    };
    script.onerror = () => {
      sdkPromise = null;
      reject(new Error('Kakao Maps SDK failed to load'));
    };
    document.head.append(script);
  });
  return sdkPromise;
}

type KakaoPlaceMapProps = {
  places: PlaceDto[];
  selectedPlace: PlaceDto | undefined;
  regionName: string;
  onSelect: (placeId: number) => void;
};

export function KakaoPlaceMap({ places, selectedPlace, regionName, onSelect }: KakaoPlaceMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<KakaoMap | null>(null);
  const sdkRef = useRef<KakaoSdk | null>(null);
  const [mapStatus, setMapStatus] = useState<'loading' | 'ready' | 'missing-key' | 'error'>('loading');
  const mapKey = import.meta.env.VITE_KAKAO_MAP_KEY;

  useEffect(() => {
    if (!mapKey) {
      setMapStatus('missing-key');
      return;
    }

    let active = true;
    setMapStatus('loading');
    loadKakaoSdk(mapKey).then((sdk) => {
      if (!active || !containerRef.current) return;
      sdkRef.current = sdk;
      const firstPlace = places.find((place) => place.lat !== null && place.lng !== null);
      const center = firstPlace
        ? new sdk.maps.LatLng(firstPlace.lat as number, firstPlace.lng as number)
        : new sdk.maps.LatLng(37.5665, 126.978);
      mapRef.current = new sdk.maps.Map(containerRef.current, { center, level: 5 });
      setMapStatus('ready');
    }).catch(() => {
      if (active) setMapStatus('error');
    });

    return () => {
      active = false;
      mapRef.current = null;
      sdkRef.current = null;
    };
  }, [mapKey]);

  useEffect(() => {
    const handleResize = () => {
      if (mapRef.current?.relayout) {
        mapRef.current.relayout();
        if (selectedPlace?.lat && selectedPlace?.lng && sdkRef.current) {
          mapRef.current.setCenter(new sdkRef.current.maps.LatLng(selectedPlace.lat, selectedPlace.lng));
        }
      }
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [selectedPlace?.lat, selectedPlace?.lng]);

  useEffect(() => {
    const map = mapRef.current;
    const sdk = sdkRef.current;
    if (mapStatus !== 'ready' || !map || !sdk) return;

    const markers: KakaoMarker[] = [];
    const infoWindows: KakaoInfoWindow[] = [];
    places.forEach((place) => {
      if (place.lat === null || place.lng === null) return;
      const position = new sdk.maps.LatLng(place.lat, place.lng);
      const marker = new sdk.maps.Marker({ map, position });
      const label = document.createElement('div');
      label.className = 'kakao-map-info';
      label.textContent = place.name;
      const infoWindow = new sdk.maps.InfoWindow({ content: label });
      markers.push(marker);
      infoWindows.push(infoWindow);
      sdk.maps.event.addListener(marker, 'click', () => onSelect(place.id));
      if (selectedPlace?.id === place.id) {
        map.setCenter(position);
        infoWindow.open(map, marker);
      }
    });

    return () => {
      markers.forEach((marker) => marker.setMap(null));
      infoWindows.forEach((infoWindow) => infoWindow.close());
    };
  }, [mapStatus, onSelect, places, selectedPlace?.id]);

  const message = {
    loading: '카카오 지도를 불러오는 중이에요.',
    ready: '',
    'missing-key': '지도 키가 설정되지 않아 목록만 표시합니다.',
    error: '지도를 불러오지 못했어요. 장소 목록은 계속 이용할 수 있어요.',
  }[mapStatus];

  return <section className="map-placeholder" aria-label={`${regionName} 지도`}>
    <div className="kakao-map-canvas" ref={containerRef} />
    {message && <div className={`map-status${mapStatus === 'error' ? ' map-status--error' : ''}`} role="status">{message}</div>}
  </section>;
}