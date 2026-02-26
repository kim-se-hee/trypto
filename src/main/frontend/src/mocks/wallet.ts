export interface WalletCoinBalance {
  coinSymbol: string;
  coinName: string;
  available: number;
  locked: number;
  currentPrice: number;
}

export interface WalletData {
  exchangeId: string;
  exchangeName: string;
  baseCurrency: string;
  walletAddress: string;
  chain: string;
  balances: WalletCoinBalance[];
}

export interface TransferRecord {
  id: string;
  exchangeId: string;
  type: "DEPOSIT" | "WITHDRAW";
  asset: string;
  amount: number;
  fee: number;
  network: string;
  address: string;
  tag?: string;
  txId?: string;
  status: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED" | "RETURNED" | "DELAYED";
  requestedAt: string;
  completedAt?: string;
}

/** 코인별 지원 체인 목록 */
export const coinChains: Record<string, string[]> = {
  BTC: ["Bitcoin", "ERC-20", "BEP-20", "TRC-20"],
  ETH: ["ERC-20", "BEP-20", "Arbitrum One"],
  XRP: ["Ripple"],
  SOL: ["Solana"],
  DOGE: ["Dogecoin", "BEP-20"],
  ADA: ["Cardano"],
  AVAX: ["Avalanche C-Chain", "BEP-20"],
  LINK: ["ERC-20", "BEP-20"],
  DOT: ["Polkadot"],
  ARB: ["Arbitrum One", "ERC-20"],
  KRW: [],
  USDT: ["ERC-20", "TRC-20", "BEP-20", "Solana", "Arbitrum One"],
};

/** 태그/메모 필요 코인 */
export const tagRequiredCoins: string[] = ["XRP", "XLM", "EOS", "ATOM"];

/** 출금 수수료: 거래소 → 코인 → 체인 → 수수료(코인 단위) */
export const withdrawalFees: Record<string, Record<string, Record<string, number>>> = {
  upbit: {
    BTC: { Bitcoin: 0.0005, "ERC-20": 0.0008, "BEP-20": 0.0002 },
    ETH: { "ERC-20": 0.005, "BEP-20": 0.0005, "Arbitrum One": 0.0003 },
    XRP: { Ripple: 1 },
    SOL: { Solana: 0.01 },
    DOGE: { Dogecoin: 5, "BEP-20": 0.5 },
    ADA: { Cardano: 1 },
    AVAX: { "Avalanche C-Chain": 0.01, "BEP-20": 0.002 },
    LINK: { "ERC-20": 0.5, "BEP-20": 0.05 },
  },
  bithumb: {
    BTC: { Bitcoin: 0.0005, "ERC-20": 0.0009, "BEP-20": 0.00025 },
    ETH: { "ERC-20": 0.005, "BEP-20": 0.0005 },
    ADA: { Cardano: 1 },
    DOT: { Polkadot: 0.1 },
  },
  binance: {
    BTC: { Bitcoin: 0.00015, "ERC-20": 0.0003, "BEP-20": 0.0000065 },
    ETH: { "ERC-20": 0.0016, "BEP-20": 0.000058, "Arbitrum One": 0.00013 },
    SOL: { Solana: 0.01 },
    LINK: { "ERC-20": 0.3, "BEP-20": 0.02 },
    ARB: { "Arbitrum One": 0.1, "ERC-20": 1.5 },
    USDT: { "ERC-20": 3.5, "TRC-20": 1, "BEP-20": 0.29, Solana: 1, "Arbitrum One": 0.1 },
  },
};

/** 거래소별 입금 주소: 거래소 → 체인 → { address, tag? } */
export const depositAddresses: Record<string, Record<string, { address: string; tag?: string }>> = {
  upbit: {
    Bitcoin: { address: "3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy" },
    "ERC-20": { address: "0x1a2B3c4D5e6F7890AbCdEf1234567890aBcDeF12" },
    "BEP-20": { address: "bnb1grpf0955h0efa6603a0b43771db4bc27d6e1r4e" },
    "TRC-20": { address: "TLa2f6VPqDgRE67v1736s7bJ8Ray5wYjU7" },
    Ripple: { address: "rPT1Sjq2YGrBMTttX4GZHjKu9dyfzbpAYe", tag: "12345678" },
    Solana: { address: "4fYNw3VBn7JtSRcLBmf5M8EPJ1K9CZyJGCz5AE3AwdHP" },
    Dogecoin: { address: "DDogepartyxxxxxxxxxxxxxxxxxxw1dfzr" },
    Cardano: { address: "addr1qxck68p7h8fjm5...r3vcfyz7smhae" },
    "Avalanche C-Chain": { address: "0x7a8b9c0D1e2F3a4B5C6D7E8F9a0b1C2D3E4f5A6b" },
  },
  bithumb: {
    Bitcoin: { address: "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq" },
    "ERC-20": { address: "0x9f8e7D6c5B4a3F2E1d0C9b8A7F6e5D4c3B2a1F0e" },
    "BEP-20": { address: "bnb1pq9f85l2k63hy5xwdpvgsx4c0h9s3k7dz2nmvf" },
    Ripple: { address: "rBithumbHotWaLLetXXXXXXXXXXX2fGq", tag: "87654321" },
    Cardano: { address: "addr1q9jk27p8e3fmn...x4cvr8ykz5qhae" },
    Polkadot: { address: "15oF4uVJwmo4TdGW7VfQxNLavjCXviqWrztPu7CAkKiA" },
  },
  binance: {
    Bitcoin: { address: "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa" },
    "ERC-20": { address: "0x28C6c06298d514Db089934071355E5743bf21d60" },
    "BEP-20": { address: "bnb136ns6lfw4zs5hg4n85vdthaad7hq5m4gtkgf23" },
    "TRC-20": { address: "TJDENsfBJs4RFETt1X1W8wMDc8M5XnJhCe" },
    Solana: { address: "2ojv9BAiHUrvsm9gxDe7fJSzbNZSJcxZvf8dqmWGHG8S" },
    "Arbitrum One": { address: "0x28C6c06298d514Db089934071355E5743bf21d60" },
  },
};

export const walletData: WalletData[] = [
  {
    exchangeId: "upbit",
    exchangeName: "업비트",
    baseCurrency: "KRW",
    walletAddress: "",
    chain: "",
    balances: [
      { coinSymbol: "KRW", coinName: "원화", available: 2_450_000, locked: 500_000, currentPrice: 1 },
      { coinSymbol: "BTC", coinName: "비트코인", available: 0.04234100, locked: 0.01000000, currentPrice: 143_250_000 },
      { coinSymbol: "ETH", coinName: "이더리움", available: 1.14500000, locked: 0.10000000, currentPrice: 4_821_000 },
      { coinSymbol: "XRP", coinName: "리플", available: 12_420.00000000, locked: 3_000.00000000, currentPrice: 3_456 },
      { coinSymbol: "SOL", coinName: "솔라나", available: 10.34000000, locked: 2.00000000, currentPrice: 287_400 },
      { coinSymbol: "DOGE", coinName: "도지코인", available: 85_000.00000000, locked: 0, currentPrice: 542 },
      { coinSymbol: "ADA", coinName: "에이다", available: 8_500.00000000, locked: 0, currentPrice: 1_234 },
      { coinSymbol: "AVAX", coinName: "아발란체", available: 45.60000000, locked: 0, currentPrice: 62_300 },
      { coinSymbol: "LINK", coinName: "체인링크", available: 120.00000000, locked: 0, currentPrice: 28_900 },
    ],
  },
  {
    exchangeId: "bithumb",
    exchangeName: "빗썸",
    baseCurrency: "KRW",
    walletAddress: "",
    chain: "",
    balances: [
      { coinSymbol: "KRW", coinName: "원화", available: 1_230_000, locked: 200_000, currentPrice: 1 },
      { coinSymbol: "BTC", coinName: "비트코인", available: 0.01800000, locked: 0.00300000, currentPrice: 143_180_000 },
      { coinSymbol: "ETH", coinName: "이더리움", available: 0.85000000, locked: 0, currentPrice: 4_815_000 },
      { coinSymbol: "ADA", coinName: "에이다", available: 10_000.00000000, locked: 2_000.00000000, currentPrice: 1_230 },
      { coinSymbol: "DOT", coinName: "폴카닷", available: 350.00000000, locked: 0, currentPrice: 12_420 },
    ],
  },
  {
    exchangeId: "binance",
    exchangeName: "바이낸스",
    baseCurrency: "USDT",
    walletAddress: "0x7a3B...F91e",
    chain: "ERC-20",
    balances: [
      { coinSymbol: "USDT", coinName: "테더", available: 4_920.50, locked: 500.00, currentPrice: 1 },
      { coinSymbol: "BTC", coinName: "Bitcoin", available: 0.10500000, locked: 0.01500000, currentPrice: 97_842.50 },
      { coinSymbol: "ETH", coinName: "Ethereum", available: 3.20000000, locked: 0.30000000, currentPrice: 3_298.75 },
      { coinSymbol: "SOL", coinName: "Solana", available: 22.00000000, locked: 3.00000000, currentPrice: 196.45 },
      { coinSymbol: "LINK", coinName: "Chainlink", available: 180.00000000, locked: 20.00000000, currentPrice: 19.78 },
      { coinSymbol: "ARB", coinName: "Arbitrum", available: 1_500.00000000, locked: 0, currentPrice: 1.8934 },
    ],
  },
];

export const transferHistory: TransferRecord[] = [
  {
    id: "upbit-1",
    exchangeId: "upbit",
    type: "WITHDRAW",
    asset: "BTC",
    amount: 0.0042,
    fee: 0.0005,
    network: "Bitcoin",
    address: "bc1qvk5q7v9wzq0w6u2us6s0dp0q3l9uc8f3m9q7d8",
    txId: "0x9bd2f1a7c7b04f9c8a1cbe3b1f5f0a1b",
    status: "PROCESSING",
    requestedAt: "2026-02-24T13:22:00Z",
  },
  {
    id: "upbit-2",
    exchangeId: "upbit",
    type: "DEPOSIT",
    asset: "ETH",
    amount: 0.85,
    fee: 0,
    network: "ERC-20",
    address: "0x1a2B3c4D5e6F7890AbCdEf1234567890aBcDeF12",
    txId: "0x73aa2e5b8c2d4f4fa1c71f4d9b3f1c55",
    status: "COMPLETED",
    requestedAt: "2026-02-20T05:40:00Z",
    completedAt: "2026-02-20T06:12:00Z",
  },
  {
    id: "upbit-3",
    exchangeId: "upbit",
    type: "WITHDRAW",
    asset: "XRP",
    amount: 320,
    fee: 1,
    network: "Ripple",
    address: "rPT1Sjq2YGrBMTttX4GZHjKu9dyfzbpAYe",
    tag: "12345678",
    txId: "0x91e9c4d9ff2b4b1dbd4d91b2b0d3cc22",
    status: "RETURNED",
    requestedAt: "2026-02-18T09:10:00Z",
    completedAt: "2026-02-18T12:30:00Z",
  },
  {
    id: "bithumb-1",
    exchangeId: "bithumb",
    type: "DEPOSIT",
    asset: "ADA",
    amount: 1200,
    fee: 0,
    network: "Cardano",
    address: "addr1q9jk27p8e3fmn0x4cvr8ykz5qhae",
    txId: "0x4b8a64b75b7d48d2b22a35c0f1a4b210",
    status: "PENDING",
    requestedAt: "2026-02-24T01:05:00Z",
  },
  {
    id: "bithumb-2",
    exchangeId: "bithumb",
    type: "WITHDRAW",
    asset: "BTC",
    amount: 0.0018,
    fee: 0.0005,
    network: "Bitcoin",
    address: "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq",
    txId: "0x2e4a5f8c1d3c4b7a9e1c1a2b3c4d5e6f",
    status: "COMPLETED",
    requestedAt: "2026-02-14T03:30:00Z",
    completedAt: "2026-02-14T04:10:00Z",
  },
  {
    id: "bithumb-3",
    exchangeId: "bithumb",
    type: "WITHDRAW",
    asset: "DOT",
    amount: 58,
    fee: 0.1,
    network: "Polkadot",
    address: "15oF4uVJwmo4TdGW7VfQxNLavjCXviqWrztPu7CAkKiA",
    txId: "0x4f7c3a8b1d2e4f5a6b7c8d9e0f1a2b3c",
    status: "FAILED",
    requestedAt: "2026-02-11T07:45:00Z",
  },
  {
    id: "binance-1",
    exchangeId: "binance",
    type: "WITHDRAW",
    asset: "USDT",
    amount: 650,
    fee: 1,
    network: "TRC-20",
    address: "TJDENsfBJs4RFETt1X1W8wMDc8M5XnJhCe",
    txId: "0x7d2c1f9a0b2c4d5e6f7a8b9c0d1e2f3a",
    status: "DELAYED",
    requestedAt: "2026-02-23T12:15:00Z",
  },
  {
    id: "binance-2",
    exchangeId: "binance",
    type: "DEPOSIT",
    asset: "SOL",
    amount: 14.2,
    fee: 0,
    network: "Solana",
    address: "2ojv9BAiHUrvsm9gxDe7fJSzbNZSJcxZvf8dqmWGHG8S",
    txId: "0x1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d",
    status: "COMPLETED",
    requestedAt: "2026-02-09T08:00:00Z",
    completedAt: "2026-02-09T08:12:00Z",
  },
  {
    id: "binance-3",
    exchangeId: "binance",
    type: "WITHDRAW",
    asset: "ARB",
    amount: 420,
    fee: 0.1,
    network: "Arbitrum One",
    address: "0x28C6c06298d514Db089934071355E5743bf21d60",
    txId: "0x5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b",
    status: "COMPLETED",
    requestedAt: "2026-01-30T10:55:00Z",
    completedAt: "2026-01-30T11:10:00Z",
  },
];
