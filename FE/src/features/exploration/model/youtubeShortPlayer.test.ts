import { describe, expect, it, vi } from 'vitest';
import { createYouTubeShortPlayer, type YouTubeApi, type YouTubePlayer } from './youtubeShortPlayer';

function setup() {
  let onReady = () => {};
  const instances: FakePlayer[] = [];
  class FakePlayer implements YouTubePlayer {
    volume = 100;
    muted = false;
    loadVideoById = vi.fn((_videoId: string) => {});
    getVolume = () => this.volume;
    setVolume = vi.fn((volume: number) => { this.volume = volume; });
    isMuted = () => this.muted;
    mute = vi.fn(() => { this.muted = true; });
    unMute = vi.fn(() => { this.muted = false; });
    destroy = vi.fn();

    constructor(_iframe: HTMLIFrameElement, options: ConstructorParameters<YouTubeApi['Player']>[1]) {
      instances.push(this);
      onReady = () => options.events.onReady({ target: this });
    }
  }

  const controller = createYouTubeShortPlayer({} as HTMLIFrameElement, 'first', { Player: FakePlayer });
  return { controller, player: instances[0], instances, ready: () => onReady() };
}

describe('YouTube shorts audio continuity', () => {
  it('starts the first video muted before playback', () => {
    const { player, ready } = setup();
    expect(player.loadVideoById).not.toHaveBeenCalled();
    ready();
    expect(player.muted).toBe(true);
    expect(player.loadVideoById).toHaveBeenCalledWith('first');
    expect(player.mute.mock.invocationCallOrder[0]).toBeLessThan(player.loadVideoById.mock.invocationCallOrder[0]);
  });

  it.each([
    { volume: 25, muted: false },
    { volume: 37, muted: true },
    { volume: 0, muted: false },
  ])('keeps native control settings $volume / muted=$muted when loading another video', ({ volume, muted }) => {
    const { controller, player, instances, ready } = setup();
    ready();
    // 사용자가 YouTube 기본 컨트롤에서 바꾼 상태를 모사한다.
    player.volume = volume;
    player.muted = muted;
    // 영상 로드가 오디오 값을 초기화해도 직전 설정을 복원해야 한다.
    player.loadVideoById.mockImplementation(() => { player.volume = 100; player.muted = false; });
    controller.loadVideo('second');
    expect(player.volume).toBe(volume);
    expect(player.muted).toBe(muted);
    expect(player.loadVideoById).toHaveBeenLastCalledWith('second');
    expect(instances).toHaveLength(1);
  });

  it('uses the latest user setting on each subsequent video', () => {
    const { controller, player, ready } = setup();
    ready();
    player.volume = 20;
    player.muted = false;
    controller.loadVideo('second');
    player.volume = 60;
    player.muted = true;
    controller.loadVideo('third');
    expect(player.volume).toBe(60);
    expect(player.muted).toBe(true);
    player.muted = false;
    controller.loadVideo('fourth');
    expect(player.volume).toBe(60);
    expect(player.muted).toBe(false);
  });

  it('loads only the latest requested video if the user advances before the player is ready', () => {
    const { controller, player, ready } = setup();
    controller.loadVideo('second');
    controller.loadVideo('third');
    expect(player.loadVideoById).not.toHaveBeenCalled();
    ready();
    expect(player.loadVideoById).toHaveBeenCalledExactlyOnceWith('third');
  });

  it('does not restart playback for updates to the same video', () => {
    const { controller, player, ready } = setup();
    ready();
    controller.loadVideo('first');
    expect(player.loadVideoById).toHaveBeenCalledTimes(1);
  });

  it('ignores late readiness and video updates after unmount', () => {
    const { controller, player, ready } = setup();
    controller.destroy();
    ready();
    controller.loadVideo('second');
    expect(player.destroy).toHaveBeenCalledTimes(1);
    expect(player.loadVideoById).not.toHaveBeenCalled();
  });
});
