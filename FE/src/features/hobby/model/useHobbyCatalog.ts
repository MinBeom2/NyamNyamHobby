import { useQuery } from '@tanstack/react-query';
import { getHobbyCatalog, hobbyQueryKeys, type HobbyCatalogParams } from '../api/hobbyApi';
import { toHobbyCardModel } from './hobbyAdapter';

export function useHobbyCatalog(params: HobbyCatalogParams = {}) {
  return useQuery({
    queryKey: hobbyQueryKeys.catalog(params),
    queryFn: ({ signal }) => getHobbyCatalog(params, signal),
    select: (hobbies) => hobbies.map(toHobbyCardModel),
  });
}
