WE'RE FUCKING COOKED

## User Profile Analysis API

`POST /api/analysis/profile`

현재 사용자 프로필을 저장 또는 갱신하고, 입력값 기반 AI 분석 결과를 반환합니다. 응답은 공통 `BaseResponse` 형식인 `success`, `message`, `data` 구조를 사용합니다.

Request:

```json
{
  "gpa": 3.8,
  "languageType": "TOEIC",
  "languageScore": 850,
  "certifications": ["정보처리기사", "SQLD"],
  "awardCount": 1,
  "project": "AI 기반 취업 분석 서비스 개발"
}
```

Response:

```json
{
  "success": true,
  "message": "사용자 프로필 분석이 완료되었습니다.",
  "data": {
    "userProfile": {
      "name": "현재 사용자",
      "gpa": 3.8,
      "language": {
        "type": "TOEIC",
        "score": 850,
        "displayText": "TOEIC 850"
      },
      "certifications": {
        "items": ["정보처리기사", "SQLD"],
        "count": 2
      },
      "awardCount": 1,
      "project": "AI 기반 취업 분석 서비스 개발"
    },
    "aiAnalysis": {
      "strengths": ["학점이 준수합니다."],
      "weaknesses": ["프로젝트 설명이 더 구체적이면 좋습니다."],
      "comment": "현재 프로필은 백엔드/데이터 직무 지원에 활용하기 좋은 구성을 가지고 있습니다."
    }
  }
}
```

Validation:

- `gpa`: `0.0` 이상 `4.5` 이하
- `languageType`: 빈 문자열 불가
- `languageScore`: 0보다 큰 숫자
- `certifications`: `정보처리기사`, `SQLD`, `ADsP`, `AWS Cloud Practitioner`, `리눅스마스터 2급`, `컴퓨터활용능력 1급`만 허용
- `awardCount`: 0 이상
- `project`: 문자열이며, 빈 값이면 분석 결과에 프로젝트 설명 보완 안내가 포함됩니다.
