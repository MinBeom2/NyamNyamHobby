import { ImageIcon } from 'lucide-react';

type MediaPlaceholderProps = {
  label?: string;
  className?: string;
  dark?: boolean;
};

export function MediaPlaceholder({ label = '이미지', className = '', dark = false }: MediaPlaceholderProps) {
  return (
    <div className={`media-placeholder ${dark ? 'media-placeholder--dark' : ''} ${className}`.trim()}>
      {label === '▶' ? (
        <span className="media-placeholder__play" aria-hidden="true">▶</span>
      ) : (
        <ImageIcon className="media-placeholder__icon" aria-hidden="true" size={16} strokeWidth={1.8} />
      )}
      <span>{label}</span>
    </div>
  );
}
