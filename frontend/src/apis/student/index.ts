import request from '@/utils/request'
import type {
  PaginationParams,
  StudentSearchCriteria,
  SearchStudentByCriteriaResponse,
  CreateStudentResponse,
  DeleteStudentResponse,
  DeactivateStudentResponse,
  FindStudentByIdResponse,
  UpdateStudentResponse,
  Student,
  RegistrationParams,
  FindStudentByTeamIdResponse
} from './types'

enum API {
  STUDENTS_ENDPOINT = '/students',
  SEARCH_STUDENTS_ENDPOINT = '/students/search'
}

export const searchStudents = (params: PaginationParams, searchCriteria: StudentSearchCriteria) =>
  request.post<any, SearchStudentByCriteriaResponse>(API.SEARCH_STUDENTS_ENDPOINT, searchCriteria, {
    params
  })

export const findStudentById = (id: number) =>
  request.get<any, FindStudentByIdResponse>(`${API.STUDENTS_ENDPOINT}/${id}`)

export const findStudentsByTeamId = (teamId: number) =>
  request.get<any, FindStudentByTeamIdResponse>(`${API.STUDENTS_ENDPOINT}/teams/${teamId}`)

export const createStudent = (newStudent: Student, params: RegistrationParams) =>
  request.post<any, CreateStudentResponse>(API.STUDENTS_ENDPOINT, newStudent, { params })

export const updateStudent = (updatedStudent: Student) =>
  request.put<any, UpdateStudentResponse>(
    `${API.STUDENTS_ENDPOINT}/${updatedStudent.id}`,
    updatedStudent
  )

// The Students page handles 403, 404 and 409 itself (UC-STU-deactivate-student extensions 3a, 5b and 5c), so they
// skip the global toast and redirect.
export const deactivateStudent = (id: number) =>
  request.put<any, DeactivateStudentResponse>(`${API.STUDENTS_ENDPOINT}/${id}/deactivate`, null, {
    handledStatuses: [403, 404, 409]
  })

export const deleteStudent = (id: number) =>
  request.delete<any, DeleteStudentResponse>(`${API.STUDENTS_ENDPOINT}/${id}`)
