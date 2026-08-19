export interface VietnamWard { code: number; name: string; }
export interface VietnamDistrict { code: number; name: string; wards: VietnamWard[]; }
export interface VietnamProvince { code: number; name: string; districts: VietnamDistrict[]; }

let cache: VietnamProvince[] | null = null;

export async function loadVietnamAddresses(): Promise<VietnamProvince[]> {
    if (cache) return cache;
    const response = await fetch('https://provinces.open-api.vn/api/v1/?depth=3');
    if (!response.ok) throw new Error('Không thể tải dữ liệu địa chỉ Việt Nam.');
    cache = await response.json() as VietnamProvince[];
    return cache;
}
