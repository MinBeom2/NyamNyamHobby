import { useEffect, useRef, useState } from 'react';
import { createYouTubeShortPlayer, loadYouTubeApi } from '../model/youtubeShortPlayer';

export function YouTubeShortPlayer({ videoId, title }: { videoId: string; title: string }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const iframeRef = useRef<HTMLIFrameElement | null>(null);
  const playerRef = useRef<ReturnType<typeof createYouTubeShortPlayer> | null>(null);
  const currentRef = useRef({ videoId, title });
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    currentRef.current = { videoId, title };
    if (iframeRef.current) iframeRef.current.title = title;
    playerRef.current?.loadVideo(videoId);
  }, [videoId, title]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    let active = true;
    const iframe = document.createElement('iframe');
    const params = new URLSearchParams({
      autoplay: '0', mute: '1', rel: '0', playsinline: '1',
      enablejsapi: '1', origin: window.location.origin,
    });
    iframe.src = `https://www.youtube-nocookie.com/embed/${encodeURIComponent(currentRef.current.videoId)}?${params}`;
    iframe.title = currentRef.current.title;
    iframe.className = 'short-player__video';
    iframe.allow = 'accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share';
    iframe.allowFullscreen = true;
    iframeRef.current = iframe;
    container.append(iframe);

    loadYouTubeApi().then((api) => {
      if (!active) return;
      playerRef.current = createYouTubeShortPlayer(iframe, currentRef.current.videoId, api);
    }).catch((reason: unknown) => {
      if (active) setError(reason instanceof Error ? reason.message : 'YouTube 플레이어를 불러오지 못했어요.');
    });

    return () => {
      active = false;
      playerRef.current?.destroy();
      playerRef.current = null;
      iframeRef.current = null;
      container.replaceChildren();
    };
  }, []);

  return <>
    <div ref={containerRef} className="short-player__video" />
    {error && <p className="short-player__copy" role="alert">{error}</p>}
  </>;
}
