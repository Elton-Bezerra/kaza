CREATE TABLE landing_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_name VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    page_path VARCHAR(500) NOT NULL,
    page_title VARCHAR(255) NOT NULL,
    location VARCHAR(100),
    field_name VARCHAR(100),
    status_code INTEGER,
    landing_path VARCHAR(500),
    referrer VARCHAR(2048),
    utm_source VARCHAR(100),
    utm_medium VARCHAR(100),
    utm_campaign VARCHAR(150),
    utm_content VARCHAR(150),
    utm_term VARCHAR(150)
);

CREATE INDEX idx_landing_events_received_at ON landing_events (received_at);
CREATE INDEX idx_landing_events_name_received_at ON landing_events (event_name, received_at);
