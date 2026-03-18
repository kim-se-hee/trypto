import { apiGet, apiPut } from "./client";

export interface UserProfileResponse {
  userId: number;
  email: string;
  nickname: string;
  portfolioPublic: boolean;
  createdAt: string;
}

export interface ChangeNicknameResponse {
  userId: number;
  nickname: string;
}

export interface ChangePortfolioVisibilityResponse {
  portfolioPublic: boolean;
}

export function getUserProfile(userId: number): Promise<UserProfileResponse> {
  return apiGet<UserProfileResponse>(`/api/users/${userId}`);
}

export function changeNickname(
  userId: number,
  nickname: string,
): Promise<ChangeNicknameResponse> {
  return apiPut<ChangeNicknameResponse>(`/api/users/${userId}/nickname`, { nickname });
}

export function changePortfolioVisibility(
  userId: number,
  isPublic: boolean,
): Promise<ChangePortfolioVisibilityResponse> {
  return apiPut<ChangePortfolioVisibilityResponse>(
    `/api/users/${userId}/portfolio-visibility`,
    { portfolioPublic: isPublic },
  );
}
