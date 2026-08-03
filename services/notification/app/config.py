from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    rabbitmq_host: str = "localhost"
    rabbitmq_port: int = 5672
    service_name: str = "notification"

    class Config:
        env_file = ".env"


settings = Settings()
