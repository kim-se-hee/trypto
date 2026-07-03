포트폴리오 스냅샷을 생성하고 그것으로 랭킹·복기를 집계하는 야간 배치의 실행 흐름. 매일 23:59 KST에 돈다. 잡 하나하나의 상세는 각 소유 컨텍스트 문서로 분리한다.

## 실행 흐름

```
23:59 KST
    SnapshotJob (스냅샷 생성)
        │
        ├─ RegretReportJob (리포트 갱신) ─┐
        │                                ├─ 병렬 실행
        └─ RankingJob (순위 매기기) ──────┘
```

- SnapshotJob이 먼저 완료된 후, RegretReportJob과 RankingJob이 병렬로 실행된다
- SnapshotJob이 실패하면 후속 잡을 실행하지 않는다
- RegretReportJob과 RankingJob은 서로 의존하지 않으며, 한쪽이 실패해도 다른 쪽은 진행한다

## 참여 잡

스냅샷이 모든 후속 잡의 기반 데이터다. 각 잡은 자신의 소유 컨텍스트 인바운드 포트만 호출한다.

| 잡 | 소유 컨텍스트 | 상세 |
|----|------------|------|
| SnapshotJob | portfolio | [snapshot-batch.md](../portfolio/snapshot-batch.md) |
| RegretReportJob | regretanalysis | [regret-report-batch.md](../regretanalysis/regret-report-batch.md) |
| RankingJob | ranking | [ranking-batch.md](../ranking/ranking-batch.md) |

## 공통 정책

- 트리거: cron `0 59 23 * * *` (Asia/Seoul)
- 중복 실행 방지: ShedLock `daily-batch` 락으로 다중 인스턴스에서 하루 1회만 실행한다
- 실패 격리: SnapshotJob 실패는 전체 중단, RegretReportJob/RankingJob 실패는 서로 격리한다
