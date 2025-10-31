package com.nutech.ppob.service;

import com.nutech.ppob.dto.content.BannerItem;
import com.nutech.ppob.dto.content.ServiceItem;
import com.nutech.ppob.repo.ContentRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContentService {

  private final ContentRepo repo;

  public ContentService(ContentRepo repo) {
    this.repo = repo;
  }

  public List<BannerItem> listBanners() {
    return repo.findActiveBanners();
  }

  public List<ServiceItem> listServices() {
    return repo.findActiveServices();
  }
}