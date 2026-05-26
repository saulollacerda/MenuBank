/**
 * Spring Data Page<T> envelope as returned by the backend.
 * Only the fields the frontend actually consumes are typed.
 */
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number // zero-based current page
  size: number
  first: boolean
  last: boolean
  empty: boolean
}

export interface PageParams {
  search?: string
  page?: number // zero-based
  size?: number
}

export const DEFAULT_PAGE_SIZE = 20
