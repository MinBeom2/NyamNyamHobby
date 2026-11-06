export type YouTubePlayer = {
  loadVideoById: (videoId: string) => void;
  getVolume: () => number;
  setVolume: (volume: number) => void;
  isMuted: () => boolean;
  mute: () => void;
  unMute: () => void;
  destroy: () => void;
};

export type YouTubeApi = {
  Player: new (iframe: HTMLIFrameElement, options: {
    events: { onReady: (event: { target: YouTubePlayer }) => void };
  }) => YouTubePlayer;
};

declare global {
  interface Window {
    YT?: YouTubeApi;
    onYouTubeIframeAPIReady?: () => void;
  }
}

let apiPromise: Promise<YouTubeApi> | null = null;

export function loadYouTubeApi(): Promise<YouTubeApi> {
  if (window.YT?.Player) return Promise.resolve(window.YT);
  if (apiPromise) return apiPromise;

  apiPromise = new Promise<YouTubeApi>((resolve, reject) => {
    const previousReady = window.onYouTubeIframeAPIReady;
    const script = document.createElement('script');
    window.onYouTubeIframeAPIReady = () => {
      window.onYouTubeIframeAPIReady = previousReady;
      if (window.YT?.Player) resolve(window.YT);
      else {
        apiPromise = null;
        reject(new Error('YouTube 플레이어를 초기화하지 못했어요.'));
      }
      previousReady?.();
    };
    script.src = 'https://www.youtube.com/iframe_api';
    script.async = true;
    script.onerror = () => {
      window.onYouTubeIframeAPIReady = previousReady;
      apiPromise = null;
      script.remove();
      reject(new Error('YouTube 플레이어를 불러오지 못했어요.'));
    };
    document.head.append(script);
  });
  return apiPromise;
}

export function createYouTubeShortPlayer(iframe: HTMLIFrameElement, initialVideoId: string, api: YouTubeApi) {
  let videoId = initialVideoId;
  let ready = false;
  let destroyed = false;
  const player = new api.Player(iframe, {
    events: {
      onReady: ({ target }) => {
        if (destroyed) return;
        ready = true;
        // 첫 진입만 음소거로 시작한다. 이후에는 같은 플레이어의 사용자 설정을 유지한다.
        target.mute();
        target.loadVideoById(videoId);
      },
    },
  });

  return {
    loadVideo(nextVideoId: string) {
      if (destroyed || videoId === nextVideoId) return;
      videoId = nextVideoId;
      if (!ready) return;

      // YouTube 기본 컨트롤에서 바꾼 최신 음량과 음소거 상태를 함께 가져온다.
      const volume = player.getVolume();
      const muted = player.isMuted();
      player.loadVideoById(videoId);
      player.setVolume(volume);
      if (muted) player.mute();
      else player.unMute();
    },
    destroy() {
      destroyed = true;
      player.destroy();
    },
  };
}
