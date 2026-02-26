export interface MockUser {
  userId: number;
  email: string;
  nickname: string;
  password: string;
  portfolioPublic: boolean;
  createdAt: string;
}

export const MOCK_USERS: MockUser[] = [
  {
    userId: 1,
    email: "test@trypto.com",
    nickname: "코인러너",
    password: "test1234",
    portfolioPublic: true,
    createdAt: "2026-01-15T09:00:00",
  },
];
