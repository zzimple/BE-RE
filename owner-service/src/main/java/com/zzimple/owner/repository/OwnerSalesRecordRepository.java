package com.zzimple.owner.repository;

import com.zzimple.owner.entity.OwnerSalesRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerSalesRecordRepository extends JpaRepository<OwnerSalesRecord, Long> {
  List<OwnerSalesRecord> findByStoreId(Long storeId);
  List<OwnerSalesRecord> findByStoreIdAndStatus(Long storeId, String status);
}
