export interface FailureRecord {
  id: string
  originalEventId: string
  originalTopic: string
  exceptionType: string
  errorMessage: string
  retryCount: number
  failedAt: string
  replayedAt: string | null
  replayEventId: string | null
}
