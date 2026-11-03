import type { HobbyDetailDto, HobbyListItemDto, NumberRangeDto } from '../api/hobbyApi';

export type HobbyCardModel = {
  id: number;
  name: string;
  category: string;
  description: string;
  thumbnailUrl: string | null;
  difficulty: string;
  place: string;
  social: string;
  duration: string;
  cost: string;
  tags: string[];
  relation: string | null;
};

const difficultyLabels = { EASY: '쉬움', NORMAL: '보통', HARD: '어려움' } as const;
const placeLabels = { INDOOR: '실내', OUTDOOR: '실외', BOTH: '상관없음' } as const;
const socialLabels = { ALONE: '혼자', TOGETHER: '함께', BOTH: '혼자 또는 함께' } as const;

function formatMinutes(minutes: number) {
  if (minutes < 60) return `${minutes}분`;
  if (minutes % 60 === 0) return `${minutes / 60}시간`;
  return `${Math.floor(minutes / 60)}시간 ${minutes % 60}분`;
}

export function formatDuration(range: NumberRangeDto) {
  return range.min === range.max
    ? `1회 ${formatMinutes(range.min)}`
    : `1회 ${formatMinutes(range.min)}~${formatMinutes(range.max)}`;
}

function formatWon(value: number) {
  if (value === 0) return '무료';
  if (value % 10000 === 0) return `${value / 10000}만 원`;
  return `${value.toLocaleString('ko-KR')}원`;
}

export function formatCost(range: NumberRangeDto) {
  return range.min === range.max ? formatWon(range.min) : `${formatWon(range.min)}~${formatWon(range.max)}`;
}

export function toHobbyCardModel(dto: HobbyListItemDto): HobbyCardModel {
  return {
    id: dto.id,
    name: dto.name,
    category: dto.category.name,
    description: dto.summary,
    thumbnailUrl: dto.thumbnailUrl,
    difficulty: difficultyLabels[dto.difficulty],
    place: placeLabels[dto.placeType],
    social: socialLabels[dto.socialType],
    duration: formatDuration(dto.sessionMinutes),
    cost: formatCost(dto.monthlyCost),
    tags: dto.tags,
    relation: dto.saveCount > 0 ? `${dto.saveCount}명이 저장` : null,
  };
}

export function toHobbyDetailModel(dto: HobbyDetailDto) {
  return {
    ...toHobbyCardModel(dto),
    longDescription: dto.description,
    initialCost: formatCost(dto.initialCost),
    monthlyCost: formatCost(dto.monthlyCost),
    supplies: dto.supplies,
    environment: dto.environment,
    firstActivity: dto.firstActivity,
    cautions: dto.cautions,
    myStatus: dto.myStatus,
  };
}
