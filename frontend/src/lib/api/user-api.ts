import { apiGet, apiPut } from "./client";

export interface UserProfileResponse {
  userId: number;
  email: string;
  nickname: string;
  createdAt: string;
}

export interface ChangeNicknameResponse {
  userId: number;
  nickname: string;
}

export function getUserProfile(): Promise<UserProfileResponse> {
  return apiGet<UserProfileResponse>("/api/users/me");
}

export function changeNickname(nickname: string): Promise<ChangeNicknameResponse> {
  return apiPut<ChangeNicknameResponse>("/api/users/me/nickname", { nickname });
}
