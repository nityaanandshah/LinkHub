// ───────────────────────────── Auth ─────────────────────────────
export interface UserInfo {
  id: number;
  email: string;
  displayName: string;
  role: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserInfo;
}

// ───────────────────────────── URLs ─────────────────────────────
export interface UrlItem {
  id: number;
  shortCode: string;
  shortUrl: string;
  longUrl: string;
  isCustomAlias: boolean;
  isActive: boolean;
  clickCount: number;
  expiresAt: string | null;
  createdAt: string;
  updatedAt: string;
  qrUrl: string;
}

export interface CreateUrlRequest {
  longUrl: string;
  customAlias?: string;
  expiresAt?: string;
}

export interface CreateUrlResponse {
  shortCode: string;
  shortUrl: string;
  longUrl: string;
  isCustomAlias: boolean;
  createdAt: string;
  expiresAt: string | null;
  qrUrl: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

// ───────────────────────────── Analytics ─────────────────────────
export interface ClickStats {
  shortCode: string;
  totalClicks: number;
  uniqueVisitors: number;
  from: string;
  to: string;
}

export interface TimeseriesPoint {
  timestamp: string;
  clicks: number;
}

export interface ReferrerStats {
  referrer: string;
  clicks: number;
  percentage: number;
}

export interface DeviceBreakdown {
  name: string;
  clicks: number;
  percentage: number;
}

export interface DeviceStats {
  deviceTypes: DeviceBreakdown[];
  browsers: DeviceBreakdown[];
  operatingSystems: DeviceBreakdown[];
}

export interface CountryData {
  country: string;
  clicks: number;
  percentage: number;
}

export interface CityData {
  city: string;
  country: string;
  clicks: number;
  latitude: number | null;
  longitude: number | null;
}

export interface GeoStats {
  countries: CountryData[];
  cities: CityData[];
}
