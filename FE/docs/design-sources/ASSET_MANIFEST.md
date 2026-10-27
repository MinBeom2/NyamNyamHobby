# Design source manifest

사용자가 전달한 디자인 원본과 런타임 에셋을 보존하는 목록이다. 원본 파일은 자르기, 색상 변경, 비율 변경 없이 `assets/`와 `screens/`에 저장한다.

## 원본 에셋

- `assets/A · 에셋 · 로고 · 캐릭터 포즈.svg`: 로고·캐릭터 포즈 전체 시트
- `assets/Component sheet.svg`: 컴포넌트·색상·타이포그래피 기준
- `assets/Logo.png`: GNB 공식 로고
- `assets/Pose=P1 인사.png`: P1 인사 포즈
- `assets/ChatGPT 이미지 2026년 10월 19일 오후 04_59_36-1 1.png`: 카메라 촬영 포즈
- `assets/ChatGPT 이미지 2026년 10월 19일 오후 04_59_41-2 1.png`: 고민 포즈
- `assets/ChatGPT 이미지 2026년 10월 19일 오후 04_59_42-3 1.png`: 사과 포즈
- `assets/ChatGPT 이미지 2026년 10월 19일 오후 04_59_43-4 1.png`: 축하 포즈
- `assets/ChatGPT 이미지 2026년 10월 19일 오후 04_59_44-5 2.png`: 가방을 찾는 포즈
- `assets/ChatGPT 이미지 2026년 10월 19일 오후 04_59_45-6 1.png`: 미술 활동 포즈

## 런타임 경로

- `/assets/brand/logo.png`: 공식 로고
- `/assets/characters/p1-greeting.png`: 인사
- `/assets/characters/p2-camera.png`: 카메라 촬영
- `/assets/characters/p3-thinking.png`: 고민
- `/assets/characters/p4-apology.png`: 사과·오류
- `/assets/characters/p5-celebration.png`: 축하·완료
- `/assets/characters/p6-searching.png`: 찾는 중
- `/assets/characters/p7-art.png`: 미술 활동

## 화면 원본

`screens/`에는 사용자가 전달한 사용자 화면 `00~09`의 PNG 및 SVG 원본 20개가 저장되어 있다. 구현 시 임의로 재구성하지 않고 해당 화면 원본과 `Component sheet.svg`를 함께 기준으로 사용한다.

## 적용 규칙

1. Figma 화면에 표시된 캐릭터 포즈를 동일하게 사용한다.
2. 로고와 캐릭터의 색, 선, 표정, 소품, 종횡비를 변경하지 않는다.
3. 캐릭터 이미지는 `object-fit: contain`으로 표시하며 강제 확대나 잘림을 금지한다.
4. 콘텐츠 사진·영상 placeholder에 캐릭터를 대신 넣지 않는다.
5. 컴포넌트 시트와 화면 SVG가 충돌하면 화면 SVG를 최신 기준으로 적용한다.
